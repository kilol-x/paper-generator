export function normalizeCitationMarker(marker, citationNo) {
  const normalizedCitationNo = citationNo ?? extractCitationNo(marker)
  if (normalizedCitationNo !== null && normalizedCitationNo !== undefined && normalizedCitationNo !== '') {
    return `【${normalizedCitationNo}】`
  }

  if (typeof marker !== 'string' || !marker.trim()) {
    return '【?】'
  }

  const compactMarker = marker.trim()
  return compactMarker
    .replace(/^\[/, '【')
    .replace(/\]$/, '】')
}

function extractCitationNo(marker) {
  if (typeof marker !== 'string') {
    return null
  }

  const matched = marker.match(/\d+/)
  return matched ? matched[0] : null
}