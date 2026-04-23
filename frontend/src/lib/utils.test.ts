import { formatDate, formatDateTime, formatJsonForDisplay, formatRepoUrlLabel } from './utils'

describe('utils', () => {
  it('formats missing dates as placeholders', () => {
    expect(formatDate(null)).toBe('-')
    expect(formatDateTime(undefined)).toBe('-')
  })

  it('formats repo urls without throwing on malformed input', () => {
    expect(formatRepoUrlLabel('https://example.com/org/repo')).toBe('org/repo')
    expect(formatRepoUrlLabel('not-a-valid-url')).toBe('not-a-valid-url')
  })

  it('pretty prints valid json and preserves invalid raw content', () => {
    expect(formatJsonForDisplay('{"ok":true}')).toBe('{\n  "ok": true\n}')
    expect(formatJsonForDisplay('not-json')).toBe('not-json')
  })
})
