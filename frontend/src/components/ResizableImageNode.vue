<script setup>
import { NodeViewWrapper } from '@tiptap/vue-3'
import { ref, onMounted } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  updateAttributes: { type: Function, required: true },
  selected: { type: Boolean, default: false }
})

const imgRef = ref(null)
const resizing = ref(false)
let currentWidth = null

// 从 node.attrs.style 解析已保存的宽度
function getWidthFromStyle() {
  const style = props.node?.attrs?.style || ''
  const match = style.match(/width:\s*([\d.]+)px/)
  return match ? parseFloat(match[1]) : null
}

onMounted(() => {
  currentWidth = getWidthFromStyle()
})

/* ==============================
   拖拽缩放逻辑
   ============================== */
function onHandleMouseDown(e) {
  e.preventDefault()
  e.stopPropagation()

  const img = imgRef.value
  if (!img) return

  const startX = e.clientX
  const startWidth = img.offsetWidth || img.naturalWidth || 200

  resizing.value = true
  document.body.style.cursor = 'nwse-resize'
  document.body.style.userSelect = 'none'

  const onMouseMove = (moveEvent) => {
    const dx = moveEvent.clientX - startX
    // 限制在编辑器内容区域内
    const container = img.closest('.editor-body')
    const containerWidth = container?.clientWidth || img.parentElement?.clientWidth || 800
    const maxWidth = containerWidth - 60 // 留一些边距
    const newWidth = Math.max(50, Math.min(startWidth + dx, maxWidth))
    currentWidth = newWidth
    img.style.width = newWidth + 'px'
    img.style.height = 'auto'
  }

  const onMouseUp = () => {
    resizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    window.removeEventListener('mousemove', onMouseMove)
    window.removeEventListener('mouseup', onMouseUp)

    if (currentWidth) {
      props.updateAttributes({
        style: `width: ${Math.round(currentWidth)}px`
      })
    }
  }

  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('mouseup', onMouseUp)
}
</script>

<template>
  <node-view-wrapper
    as="span"
    class="resizable-image-wrapper"
    :class="{ 'is-selected': selected, 'is-resizing': resizing }"
  >
    <img
      ref="imgRef"
      :src="node.attrs.src"
      :alt="node.attrs.alt"
      :title="node.attrs.title"
      :style="node.attrs.style"
      draggable="false"
    />
    <span
      v-if="selected"
      class="resize-handle"
      title="拖拽调整图片大小"
      @mousedown="onHandleMouseDown"
    >
      <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
        <path
          d="M1 9L9 1M9 9V5.5M9 9H5.5"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </span>
  </node-view-wrapper>
</template>
