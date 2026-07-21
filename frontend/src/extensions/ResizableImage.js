import Image from '@tiptap/extension-image'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import ResizableImageNode from '../components/ResizableImageNode.vue'

/**
 * 基于 @tiptap/extension-image 扩展的自定义图片扩展
 * 增加 style attribute 用于存储拖拽调整后的宽度
 * 通过 VueNodeViewRenderer 渲染自定义的 ResizableImageNode 组件
 */
export const ResizableImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      style: {
        default: '',
        renderHTML: (attributes) => {
          if (!attributes.style) return {}
          return { style: attributes.style }
        }
      }
    }
  },

  addNodeView() {
    return VueNodeViewRenderer(ResizableImageNode)
  }
})
