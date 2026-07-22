export function normalizeCitationMarker(marker, citationNo, numberFormat = '[N]') {
  const normalizedCitationNo = citationNo ?? extractCitationNo(marker)
  const template = typeof numberFormat === 'string' && numberFormat.includes('N')
    ? numberFormat
    : '[N]'

  if (normalizedCitationNo !== null && normalizedCitationNo !== undefined && normalizedCitationNo !== '') {
    return template.replace(/N/g, String(normalizedCitationNo))
  }

  if (typeof marker === 'string' && marker.trim()) {
    return marker.trim()
  }

  return template.replace(/N/g, '?')
}

export function stripLeadingCitationMarker(text) {
  if (typeof text !== 'string') {
    return ''
  }

  return text
    .replace(/^\s*(?:[\[【(（]\s*\d+\s*[\]】)）]|\d+[.、])\s*/, '')
    .trim()
}

export function normalizeCitationTagsHtml(html, numberFormat = '[N]') {
  if (typeof html !== 'string' || !html.includes('data-citation-tag')) {
    return normalizePlainCitationText(html || '', numberFormat)
  }

  const container = document.createElement('div')
  container.innerHTML = html

  container.querySelectorAll('span[data-citation-tag]').forEach(node => {
    const citationNo = node.getAttribute('citationno')
      || node.getAttribute('citationNo')
      || extractCitationNo(node.getAttribute('marker') || node.textContent || '')
    const marker = normalizeCitationMarker(node.getAttribute('marker') || node.textContent || '', citationNo, numberFormat)
    node.setAttribute('marker', marker)
    node.setAttribute('label', marker)
    node.setAttribute('numberFormat', numberFormat)
    node.textContent = marker
  })

  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT)
  const textNodes = []
  let currentNode = walker.nextNode()
  while (currentNode) {
    textNodes.push(currentNode)
    currentNode = walker.nextNode()
  }

  textNodes.forEach(node => {
    if (node.parentElement?.closest('[data-citation-tag]')) {
      return
    }
    node.textContent = normalizePlainCitationText(node.textContent || '', numberFormat)
  })

  return container.innerHTML
}

function normalizePlainCitationText(text, numberFormat) {
  if (typeof text !== 'string' || !text) {
    return text || ''
  }

  const markerPattern = /(\[(\d+)\]|【(\d+)】|\((\d+)\)|（(\d+)）)/g

  return text.replace(markerPattern, (matched, sq, cn, en, full) => {
    const citationNo = sq || cn || en || full
    return normalizeCitationMarker('', citationNo, numberFormat)
  })
}

function extractCitationNo(marker) {
  if (typeof marker !== 'string') {
    return null
  }

  const matched = marker.match(/\d+/)
  return matched ? matched[0] : null
}