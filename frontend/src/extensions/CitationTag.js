import { Node, mergeAttributes } from '@tiptap/core'
import { normalizeCitationMarker } from '../utils/citation.js'

export const CitationTag = Node.create({
  name: 'citationTag',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      marker: { default: '【1】' },
      label: { default: '' },
      referenceId: { default: null },
      citationNo: { default: null },
      year: { default: '' },
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-citation-tag]' }]
  },

  renderHTML({ HTMLAttributes }) {
    const displayMarker = normalizeCitationMarker(HTMLAttributes.marker, HTMLAttributes.citationNo)
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        marker: displayMarker,
        'data-citation-tag': 'true',
        class: 'citation-tag-node',
        contenteditable: 'false'
      }),
      displayMarker
    ]
  },

  addCommands() {
    return {
      insertCitationTag: attributes => ({ commands }) => commands.insertContent({
        type: this.name,
        attrs: attributes
      })
    }
  }
})