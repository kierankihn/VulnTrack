#!/bin/bash
# Example: Run OWASP Dependency-Check and push results to VulnTrack
# Usage: ./dependency-check-report.sh <project-name>

set -e

PROJECT_NAME="${1:-my-project}"
VULNTRACK_URL="${VULNTRACK_URL:-http://localhost:8080}"
VULNTRACK_API_KEY="${VULNTRACK_API_KEY:-changeme}"
REPORT_DIR="./dependency-check-report"

echo "Running OWASP Dependency-Check for project: $PROJECT_NAME"

# Run Dependency-Check (adjust path as needed)
dependency-check \
  --project "$PROJECT_NAME" \
  --scan . \
  --format JSON \
  --out "$REPORT_DIR" \
  --enableRetired

REPORT_FILE="$REPORT_DIR/dependency-check-report.json"

if [ ! -f "$REPORT_FILE" ]; then
  echo "ERROR: Report file not found at $REPORT_FILE"
  exit 1
fi

echo "Transforming report for VulnTrack API..."

# Transform Dependency-Check JSON report to VulnTrack format using jq
PAYLOAD=$(jq --arg project "$PROJECT_NAME" '{
  projectName: $project,
  dependencies: [
    .dependencies[]
    | select(.vulnerabilities != null and (.vulnerabilities | length) > 0)
    | {
        fileName: .fileName,
        filePath: .filePath,
        vulnerabilities: [
          .vulnerabilities[]
          | select(.name | startswith("CVE-"))
          | {
              name: .name,
              severity: .severity,
              cvssScore: (.cvssv3.baseScore // .cvssv2.score // 0),
              description: .description,
              references: [.references[]?.url // empty]
            }
        ]
      }
  ]
}' "$REPORT_FILE")

echo "Sending report to VulnTrack..."

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "$VULNTRACK_URL/api/v1/external/scan" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $VULNTRACK_API_KEY" \
  -d "$PAYLOAD")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
  TICKETS_CREATED=$(echo "$BODY" | jq '.ticketsCreated')
  echo "SUCCESS: $TICKETS_CREATED new ticket(s) created in VulnTrack"
  echo "$BODY" | jq '.tickets[] | "  - #\(.id): \(.title) [\(.status)]"' -r 2>/dev/null || true
else
  echo "ERROR: VulnTrack API returned HTTP $HTTP_CODE"
  echo "$BODY"
  exit 1
fi
