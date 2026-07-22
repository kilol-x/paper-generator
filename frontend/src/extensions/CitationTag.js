import { Node, mergeAttributes } from '@tiptap/core'

export const CitationTag = Node.create({
  name: 'citationTag',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      marker: { default: '[1]' },
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
    const label = HTMLAttributes.label || HTMLAttributes.marker || '[?]'
    return [
      'span',
      mergeAttributes(HTMLAttributes, {
        'data-citation-tag': 'true',
        class: 'citation-tag-node',
        contenteditable: 'false'
      }),
      label
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