package com.vulntrack.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScanReportDto {

    private String projectName;
    private String repoUrl;
    private Long assetId;
    private List<Dependency> dependencies;

    @Data
    public static class Dependency {
        private String fileName;
        private String filePath;
        private List<Vulnerability> vulnerabilities;
    }

    @Data
    public static class Vulnerability {
        private String name;
        private String severity;
        private Double cvssScore;
        private String description;
        private List<String> references;
    }
}
