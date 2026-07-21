<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'insert'])

// ==================== 快速模板 ====================
const TEMPLATES = [
  {
    key: 'three-line',
    name: '三线表',
    desc: '3列 × 3行',
    rows: 3, cols: 3,
    data: [
      ['变量', '均值', '标准差'],
      ['X₁', '3.45', '0.89'],
      ['X₂', '2.17', '1.02']
    ]
  },
  {
    key: 'compare',
    name: '对比表',
    desc: '2列 × 4行',
    rows: 4, cols: 2,
    data: [
      ['指标', '数值'],
      ['实验组', '85.3'],
      ['对照组', '72.1'],
      ['差值', '13.2*']
    ]
  },
  {
    key: 'multi',
    name: '多列表',
    desc: '4列 × 5行',
    rows: 5, cols: 4,
    data: [
      ['', '', '', ''],
      ['', '', '', ''],
      ['', '', '', ''],
      ['', '', '', ''],
      ['', '', '', '']
    ]
  }
]

// ==================== 状态 ====================
const mode = ref('visual')         // 'visual' | 'simple' | 'code'
const isThreeLine = ref(true)
const tableTitle = ref('')
const tableNote = ref('')
const rows = ref(3)
const cols = ref(3)
const cells = ref([])              // cells[row][col]

const codeText = ref('')           // 代码模式下的 HTML
const showLatex = ref(false)       // LaTeX 预览折叠

// 可视化模式下当前聚焦的单元格
const focusedCell = ref({ row: -1, col: -1 })

// 简易模式文本
const simpleText = ref('')

// ==================== 单元格初始化 ====================
function initCells(r, c, preset) {
  rows.value = r
  cols.value = c
  const arr = []
  for (let i = 0; i < r; i++) {
    arr[i] = []
    for (let j = 0; j < c; j++) {
      arr[i][j] = (preset && preset[i] !== undefined && preset[i][j] !== undefined)
        ? preset[i][j]
        : ''
    }
  }
  cells.value = arr
}

// ==================== 模板套用 ====================
function applyTemplate(tpl) {
  initCells(tpl.rows, tpl.cols, tpl.data)
  isThreeLine.value = true
  tableTitle.value = ''
  tableNote.value = ''
  mode.value = 'visual'
  focusedCell.value = { row: -1, col: -1 }
}

// ==================== 行列操作 ====================
function addRow() {
  const newRow = new Array(cols.value).fill('')
  cells.value.push(newRow)
  rows.value++
}

function addCol() {
  for (let i = 0; i < rows.value; i++) {
    cells.value[i].push('')
  }
  cols.value++
}

function deleteRow() {
  if (rows.value <= 1) return
  cells.value.pop()
  rows.value--
  if (focusedCell.value.row >= rows.value) {
    focusedCell.value = { row: -1, col: -1 }
  }
}

function deleteCol() {
  if (cols.value <= 1) return
  for (let i = 0; i < rows.value; i++) {
    cells.value[i].pop()
  }
  cols.value--
  if (focusedCell.value.col >= cols.value) {
    focusedCell.value = { row: -1, col: -1 }
  }
}

// ==================== 单元格交互 ====================
function focusCell(r, c) {
  focusedCell.value = { row: r, col: c }
}

function blurCell() {
  focusedCell.value = { row: -1, col: -1 }
}

function onCellKeydown(e, r, c) {
  if (e.key === 'Tab') {
    e.preventDefault()
    const dir = e.shiftKey ? -1 : 1
    const flat = r * cols.value + c + dir
    const total = rows.value * cols.value
    const next = ((flat % total) + total) % total
    const nr = Math.floor(next / cols.value)
    const nc = next % cols.value
    focusCell(nr, nc)
    // 聚焦对应 input
    nextTick(() => {
      const el = document.querySelector(`[data-cell-r="${nr}"][data-cell-c="${nc}"]`)
      el?.focus()
    })
  }
  if (e.key === 'ArrowUp' && r > 0) {
    e.preventDefault()
    focusCell(r - 1, c)
    nextTick(() => {
      const el = document.querySelector(`[data-cell-r="${r - 1}"][data-cell-c="${c}"]`)
      el?.focus()
    })
  }
  if (e.key === 'ArrowDown' && r < rows.value - 1) {
    e.preventDefault()
    focusCell(r + 1, c)
    nextTick(() => {
      const el = document.querySelector(`[data-cell-r="${r + 1}"][data-cell-c="${c}"]`)
      el?.focus()
    })
  }
}

// ==================== 简易模式同步 ====================
function syncFromSimple() {
  const lines = simpleText.value.split('\n')
  const newData = lines.map(line => {
    // 支持 tab 分隔
    return line.split('\t')
  })
  // 标准化列数
  const maxCols = Math.max(...newData.map(r => r.length), 1)
  for (const row of newData) {
    while (row.length < maxCols) row.push('')
  }
  cells.value = newData
  rows.value = newData.length
  cols.value = maxCols
}

function syncToSimple() {
  simpleText.value = cells.value.map(row => row.join('\t')).join('\n')
}

// ==================== 代码模式同步 ====================
function syncFromCode() {
  // 简单解析：尝试从 HTML 中提取单元格内容
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(codeText.value, 'text/html')
    const table = doc.querySelector('table')
    if (!table) return

    const allRows = table.querySelectorAll('tr')
    const newData = []
    let maxC = 0
    allRows.forEach(tr => {
      const rowData = []
      tr.querySelectorAll('th, td').forEach(cell => {
        rowData.push(cell.textContent || '')
      })
      if (rowData.length > 0) {
        newData.push(rowData)
        maxC = Math.max(maxC, rowData.length)
      }
    })
    if (newData.length > 0) {
      for (const row of newData) {
        while (row.length < maxC) row.push('')
      }
      cells.value = newData
      rows.value = newData.length
      cols.value = maxC

      // 尝试读取标题
      const caption = table.querySelector('caption')
      if (caption) tableTitle.value = caption.textContent || ''
    }
  } catch { /* 解析失败，保持原样 */ }
}

function syncToCode() {
  codeText.value = generateHtml()
}

// ==================== HTML 生成 ====================
function generateHtml() {
  const cls = isThreeLine.value ? 'table-three-line' : ''
  let html = ''

  // 表格标题
  if (tableTitle.value.trim()) {
    html += `<p style="text-align:center;font-weight:600;margin:0 0 6px;">${tableTitle.value.trim()}</p>`
  }

  html += `<table class="${cls}" style="border-collapse:collapse;width:100%;margin:12px auto;">`

  // thead（第一行作为表头）
  if (rows.value > 0) {
    html += '<thead><tr>'
    for (let j = 0; j < cols.value; j++) {
      html += `<th style="padding:6px 10px;text-align:center;`
      if (isThreeLine.value) {
        html += `border-top:1.5pt solid #000;border-bottom:0.75pt solid #000;`
      } else {
        html += `border:1px solid #333;background:#fafaf8;`
      }
      html += `font-weight:600;font-size:10.5pt;">${cells.value[0][j] || ''}</th>`
    }
    html += '</tr></thead>'
  }

  // tbody
  if (rows.value > 1) {
    html += '<tbody>'
    for (let i = 1; i < rows.value; i++) {
      html += '<tr>'
      for (let j = 0; j < cols.value; j++) {
        let borderStyle
        if (isThreeLine.value) {
          borderStyle = (i === rows.value - 1)
            ? 'border-bottom:1.5pt solid #000;'
            : 'border:none;'
        } else {
          borderStyle = 'border:1px solid #333;'
        }
        html += `<td style="padding:6px 10px;text-align:center;font-size:10.5pt;${borderStyle}">${cells.value[i][j] || ''}</td>`
      }
      html += '</tr>'
    }
    html += '</tbody>'
  }

  html += '</table>'

  // 表注
  if (tableNote.value.trim()) {
    html += `<p style="font-size:9pt;text-align:left;margin-top:4px;color:#555;line-height:1.4;">${tableNote.value.trim()}</p>`
  }

  return html
}

// ==================== LaTeX 预览 ====================
const latexPreview = computed(() => {
  const lines = []
  lines.push('\\begin{table}[htbp]')
  lines.push('  \\centering')
  if (tableTitle.value.trim()) {
    lines.push(`  \\caption{${tableTitle.value.trim()}}`)
    lines.push(`  \\label{tab:1}`)
  }
  const colSpec = 'c'.repeat(cols.value)
  lines.push(`  \\begin{tabular}{${colSpec}}`)
  if (isThreeLine.value) lines.push('    \\toprule')
  for (let i = 0; i < rows.value; i++) {
    const rowCells = cells.value[i].map(c => c || '').join(' & ')
    lines.push(`    ${rowCells} \\\\`)
    if (i === 0 && isThreeLine.value) lines.push('    \\midrule')
  }
  if (isThreeLine.value) lines.push('    \\bottomrule')
  lines.push('  \\end{tabular}')
  if (tableNote.value.trim()) {
    lines.push(`  \\floatnote{${tableNote.value.trim()}}`)
  }
  lines.push('\\end{table}')
  return lines.join('\n')
})

// ==================== 切换模式 ====================
function switchMode(m) {
  // 离开当前模式前同步
  if (mode.value === 'simple') syncFromSimple()
  if (mode.value === 'code') syncFromCode()

  mode.value = m

  // 进入新模式后同步
  if (m === 'simple') syncToSimple()
  if (m === 'code') syncToCode()
}

// ==================== 操作按钮 ====================
function handleReset() {
  initCells(3, 3, null)
  tableTitle.value = ''
  tableNote.value = ''
  isThreeLine.value = true
  mode.value = 'visual'
  focusedCell.value = { row: -1, col: -1 }
}

function handleCancel() {
  emit('update:modelValue', false)
}

function handleInsert() {
  // 确保最新数据
  if (mode.value === 'simple') syncFromSimple()
  if (mode.value === 'code') syncFromCode()

  const html = mode.value === 'code' ? (codeText.value || generateHtml()) : generateHtml()
  emit('insert', html)
  emit('update:modelValue', false)
}

// ==================== 对话框打开时初始化 ====================
watch(() => props.modelValue, (v) => {
  if (v) {
    initCells(3, 3, null)
    tableTitle.value = ''
    tableNote.value = ''
    isThreeLine.value = true
    mode.value = 'visual'
    focusedCell.value = { row: -1, col: -1 }
    showLatex.value = false
  }
})

// ==================== 表头列标识 ====================
const colLetters = computed(() => {
  const letters = []
  for (let i = 0; i < cols.value; i++) {
    letters.push(String.fromCharCode(65 + i))
  }
  return letters
})
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="te-overlay" @click.self="handleCancel">
      <div class="te-dialog">
        <!-- 标题栏 -->
        <div class="te-header">
          <h3 class="te-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-4px;margin-right:6px">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
            插入表格
          </h3>
          <button class="te-close" @click="handleCancel" title="关闭">&times;</button>
        </div>

        <div class="te-body">
          <!-- ====== 快速模板区 ====== -->
          <div class="te-section">
            <div class="te-section-label">快速模板</div>
            <div class="te-templates">
              <button
                v-for="tpl in TEMPLATES"
                :key="tpl.key"
                class="te-tpl-card"
                @click="applyTemplate(tpl)"
              >
                <!-- 缩略预览 -->
                <div class="te-tpl-thumb" :class="{ three: isThreeLine }">
                  <table>
                    <thead>
                      <tr>
                        <th v-for="j in Math.min(tpl.cols, 3)" :key="j"></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="i in Math.min(tpl.rows - 1, 2)" :key="i">
                        <td v-for="j in Math.min(tpl.cols, 3)" :key="j"></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div class="te-tpl-info">
                  <span class="te-tpl-name">{{ tpl.name }}</span>
                  <span class="te-tpl-desc">{{ tpl.desc }}</span>
                </div>
              </button>
            </div>
          </div>

          <!-- ====== 编辑模式切换 ====== -->
          <div class="te-section te-mode-row">
            <div class="te-mode-tabs">
              <button
                v-for="m in [
                  { key: 'visual', label: '可视化编辑', icon: '⊞' },
                  { key: 'simple', label: '简易模式', icon: '☰' },
                  { key: 'code',   label: '代码模式', icon: '<>' }
                ]"
                :key="m.key"
                :class="{ active: mode === m.key }"
                @click="switchMode(m.key)"
              >
                <span class="te-mode-icon">{{ m.icon }}</span>
                {{ m.label }}
              </button>
            </div>

            <!-- 三线表开关 -->
            <label class="te-three-line-toggle">
              <input type="checkbox" v-model="isThreeLine" />
              <span class="te-toggle-label">三线表格式</span>
            </label>
          </div>

          <!-- ====== 表格标题 ====== -->
          <div class="te-section te-title-row">
            <label class="te-field">
              <span class="te-field-label">表格标题</span>
              <input
                v-model="tableTitle"
                type="text"
                placeholder="例：表1-1 各变量描述性统计"
                class="te-input"
              />
            </label>
          </div>

          <!-- ====== 可视化编辑模式 ====== -->
          <div v-if="mode === 'visual'" class="te-section te-table-wrap">
            <!-- 列标识 -->
            <div class="te-col-header-row">
              <span class="te-row-num"></span>
              <span v-for="(letter, j) in colLetters" :key="j" class="te-col-letter">{{ letter }}</span>
              <span class="te-col-action-spacer"></span>
            </div>

            <div v-for="(row, i) in cells" :key="i" class="te-table-row">
              <span class="te-row-num">{{ i + 1 }}</span>
              <input
                v-for="(cell, j) in row"
                :key="j"
                :data-cell-r="i"
                :data-cell-c="j"
                :class="[
                  'te-cell-input',
                  { 'header-cell': i === 0, 'focused': focusedCell.row === i && focusedCell.col === j }
                ]"
                :value="cell"
                @input="cells[i][j] = $event.target.value"
                @focus="focusCell(i, j)"
                @blur="blurCell"
                @keydown="onCellKeydown($event, i, j)"
                :placeholder="i === 0 ? '表头' : ''"
              />
              <!-- 行删除按钮 -->
              <button
                v-if="rows > 1"
                class="te-row-del"
                title="删除此行"
                @click="deleteRow(); rows > 1 && i >= rows && null"
              >&times;</button>
              <span v-else class="te-col-action-spacer"></span>
            </div>

            <!-- 列删除按钮行 -->
            <div class="te-col-del-row">
              <span class="te-row-num"></span>
              <button
                v-for="j in cols"
                :key="'del-' + j"
                v-show="cols > 1"
                class="te-col-del"
                title="删除此列"
                @click="deleteCol()"
              >&times;</button>
            </div>
          </div>

          <!-- ====== 简易模式 ====== -->
          <div v-if="mode === 'simple'" class="te-section">
            <div class="te-simple-hint">
              每行代表表格的一行，单元格之间用 <strong>Tab</strong> 键分隔。编辑完成后切换到"可视化编辑"查看效果。
            </div>
            <textarea
              v-model="simpleText"
              class="te-textarea te-simple-textarea"
              rows="8"
              placeholder="变量	均值	标准差
X1	3.45	0.89
X2	2.17	1.02"
              @blur="syncFromSimple"
            ></textarea>
          </div>

          <!-- ====== 代码模式 ====== -->
          <div v-if="mode === 'code'" class="te-section">
            <div class="te-simple-hint">
              直接编辑表格的 HTML 代码。支持 <code>&lt;table&gt;</code>、<code>&lt;caption&gt;</code>、<code>&lt;thead&gt;</code>、<code>&lt;tbody&gt;</code> 等标签。
            </div>
            <textarea
              v-model="codeText"
              class="te-textarea te-code-textarea"
              rows="10"
              spellcheck="false"
              @blur="syncFromCode"
            ></textarea>
          </div>

          <!-- ====== 表格控制按钮 ====== -->
          <div class="te-section te-controls" v-if="mode === 'visual'">
            <button class="te-ctrl-btn" @click="addRow" title="在底部增加一行">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              加行
            </button>
            <button class="te-ctrl-btn" @click="addCol" title="在右侧增加一列">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              加列
            </button>
            <button class="te-ctrl-btn te-ctrl-danger" @click="deleteRow" :disabled="rows <= 1" title="删除最后一行">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
              删行
            </button>
            <button class="te-ctrl-btn te-ctrl-danger" @click="deleteCol" :disabled="cols <= 1" title="删除最后一列">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
              删列
            </button>
            <span class="te-ctrl-info"> {{ rows }} 行 × {{ cols }} 列</span>
          </div>

          <!-- ====== 表注 ====== -->
          <div class="te-section">
            <label class="te-field">
              <span class="te-field-label">表注</span>
              <input
                v-model="tableNote"
                type="text"
                placeholder="例：*p&lt;0.1, **p&lt;0.05, ***p&lt;0.01"
                class="te-input"
              />
            </label>
          </div>

          <!-- ====== LaTeX 预览（可折叠） ====== -->
          <div class="te-section">
            <button class="te-latex-toggle" @click="showLatex = !showLatex">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:4px;transition:transform .2s" :style="{ transform: showLatex ? 'rotate(90deg)' : '' }"><polyline points="9 18 15 12 9 6"/></svg>
              LaTeX 预览
            </button>
            <pre v-if="showLatex" class="te-latex-code">{{ latexPreview }}</pre>
          </div>
        </div>

        <!-- ====== 底部按钮 ====== -->
        <div class="te-footer">
          <button class="te-btn te-btn-reset" @click="handleReset">重 置</button>
          <div class="te-footer-right">
            <button class="te-btn te-btn-cancel" @click="handleCancel">取 消</button>
            <button class="te-btn te-btn-insert" @click="handleInsert">插入表格</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* ================================================================
   TableEditor — 表格插入弹窗（莫兰迪风格）
   ================================================================ */

/* ---- 遮罩 ---- */
.te-overlay {
  position: fixed;
  inset: 0;
  background: rgba(36, 38, 34, 0.45);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

/* ---- 对话框 ---- */
.te-dialog {
  width: min(900px, 100%);
  max-height: 90vh;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ---- 标题栏 ---- */
.te-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px solid #e4e3de;
  flex-shrink: 0;
}
.te-title {
  margin: 0;
  font-size: 17px;
  color: #242622;
  font-family: var(--font-heading, "Noto Serif SC", serif);
}
.te-close {
  width: 32px; height: 32px;
  border: none;
  background: transparent;
  font-size: 22px;
  color: #858982;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
}
.te-close:hover { background: #f0f1ed; color: #242622; }

/* ---- 内容区 ---- */
.te-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 22px;
}
.te-body::-webkit-scrollbar { width: 6px; }
.te-body::-webkit-scrollbar-thumb { background: #d0d3ce; border-radius: 999px; }

.te-section {
  margin-bottom: 14px;
}
.te-section-label {
  font-size: 12px;
  font-weight: 700;
  color: #858982;
  margin-bottom: 8px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

/* ---- 快速模板卡片 ---- */
.te-templates {
  display: flex;
  gap: 10px;
}
.te-tpl-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 2px solid #e4e3de;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all .18s;
  text-align: left;
}
.te-tpl-card:hover {
  border-color: #4f776a;
  background: #f4f7f5;
}
.te-tpl-thumb {
  width: 52px;
  flex-shrink: 0;
}
.te-tpl-thumb table {
  width: 100%;
  border-collapse: collapse;
  font-size: 6px;
}
.te-tpl-thumb th,
.te-tpl-thumb td {
  border: 1px solid #ccc;
  padding: 2px 3px;
  min-width: 12px;
  height: 8px;
  background: #fafaf8;
}
.te-tpl-thumb th { background: #e8ece9; }
.te-tpl-name {
  display: block;
  font-weight: 600;
  font-size: 14px;
  color: #242622;
}
.te-tpl-desc {
  font-size: 11px;
  color: #858982;
}

/* ---- 模式切换 ---- */
.te-mode-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.te-mode-tabs {
  display: flex;
  gap: 2px;
  background: #f0f1ed;
  border-radius: 8px;
  padding: 3px;
}
.te-mode-tabs button {
  padding: 7px 14px;
  border: none;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: #858982;
  border-radius: 6px;
  cursor: pointer;
  transition: all .15s;
  white-space: nowrap;
}
.te-mode-tabs button:hover { color: #4f776a; }
.te-mode-tabs button.active {
  background: #fff;
  color: #4f776a;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
}
.te-mode-icon {
  margin-right: 4px;
  font-family: monospace;
}

/* ---- 三线表开关 ---- */
.te-three-line-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #5c605a;
  font-weight: 500;
  user-select: none;
}
.te-three-line-toggle input[type="checkbox"] {
  width: 16px; height: 16px;
  accent-color: #4f776a;
  cursor: pointer;
}
.te-toggle-label { cursor: pointer; }

/* ---- 字段 ---- */
.te-field {
  display: flex;
  align-items: center;
  gap: 10px;
}
.te-field-label {
  font-size: 13px;
  font-weight: 500;
  color: #5c605a;
  flex-shrink: 0;
  min-width: 56px;
}
.te-input {
  flex: 1;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #d8d9d4;
  border-radius: 7px;
  font-size: 13px;
  color: #242622;
  background: #fafaf8;
  outline: none;
  transition: border-color .15s, box-shadow .15s;
}
.te-input:focus {
  border-color: #4f776a;
  box-shadow: 0 0 0 3px rgba(79, 119, 106, 0.08);
}
.te-input::placeholder { color: #c0c3bd; }

/* ---- 表格编辑区 ---- */
.te-table-wrap {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: #fafaf8;
  border: 1px solid #e4e3de;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
}

/* 列标识行 */
.te-col-header-row,
.te-table-row {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-bottom: 3px;
}
.te-col-letter {
  width: 90px;
  text-align: center;
  font-size: 10px;
  color: #858982;
  font-weight: 600;
  flex-shrink: 0;
}
.te-row-num {
  width: 22px;
  text-align: center;
  font-size: 10px;
  color: #b0b3ae;
  font-weight: 600;
  flex-shrink: 0;
}

/* 单元格输入框 */
.te-cell-input {
  width: 90px;
  height: 34px;
  padding: 4px 8px;
  border: 1px solid #d8d9d4;
  border-radius: 4px;
  font-size: 12px;
  color: #242622;
  text-align: center;
  background: #fff;
  outline: none;
  flex-shrink: 0;
  transition: border-color .12s, box-shadow .12s;
}
.te-cell-input:focus,
.te-cell-input.focused {
  border-color: #4f776a;
  box-shadow: 0 0 0 2px rgba(79, 119, 106, 0.12);
  z-index: 1;
  position: relative;
}
.te-cell-input.header-cell {
  font-weight: 600;
  background: #eef4f1;
  font-size: 12px;
}
.te-cell-input::placeholder {
  color: #ccc;
  font-weight: 400;
}

/* 行删除 / 列删除 */
.te-row-del,
.te-col-del {
  width: 24px; height: 24px;
  border: none;
  background: transparent;
  color: #c0c3bd;
  font-size: 16px;
  cursor: pointer;
  border-radius: 4px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .12s;
}
.te-row-del:hover,
.te-col-del:hover { color: #a8544d; background: #faeaea; }
.te-col-action-spacer { width: 24px; flex-shrink: 0; }

/* 列删除行 */
.te-col-del-row {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 2px;
}
.te-col-del-row .te-col-del {
  width: 90px;
  height: 20px;
  font-size: 14px;
}

/* ---- 简易 & 代码文本框 ---- */
.te-textarea {
  width: 100%;
  border: 1px solid #d8d9d4;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.6;
  font-family: "Consolas", "Courier New", monospace;
  color: #242622;
  background: #fafaf8;
  outline: none;
  resize: vertical;
  transition: border-color .15s, box-shadow .15s;
}
.te-textarea:focus {
  border-color: #4f776a;
  box-shadow: 0 0 0 3px rgba(79, 119, 106, 0.08);
}
.te-simple-textarea {
  min-height: 160px;
}
.te-code-textarea {
  min-height: 200px;
  white-space: pre;
  overflow-wrap: normal;
  overflow-x: auto;
}
.te-simple-hint {
  font-size: 11px;
  color: #858982;
  margin-bottom: 6px;
  line-height: 1.5;
}
.te-simple-hint code {
  background: #e8ece9;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 11px;
}

/* ---- 控制按钮 ---- */
.te-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.te-ctrl-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #d8d9d4;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  color: #5c605a;
  cursor: pointer;
  transition: all .12s;
}
.te-ctrl-btn:hover {
  border-color: #4f776a;
  color: #4f776a;
  background: #f4f7f5;
}
.te-ctrl-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.te-ctrl-btn:disabled:hover {
  border-color: #d8d9d4;
  color: #5c605a;
  background: #fff;
}
.te-ctrl-danger:hover:not(:disabled) {
  border-color: #a8544d;
  color: #a8544d;
  background: #fdf4f3;
}
.te-ctrl-info {
  font-size: 11px;
  color: #b0b3ae;
  margin-left: auto;
}

/* ---- LaTeX 预览 ---- */
.te-latex-toggle {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border: none;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  color: #858982;
  cursor: pointer;
  border-radius: 6px;
  transition: all .12s;
}
.te-latex-toggle:hover { color: #4f776a; background: #f4f7f5; }
.te-latex-code {
  margin: 8px 0 0;
  padding: 14px 16px;
  background: #2d2f2a;
  color: #d4d9d0;
  font-size: 12px;
  line-height: 1.7;
  font-family: "Consolas", "Courier New", monospace;
  border-radius: 8px;
  overflow-x: auto;
  white-space: pre;
}

/* ---- 底部按钮 ---- */
.te-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 22px;
  border-top: 1px solid #e4e3de;
  flex-shrink: 0;
}
.te-footer-right {
  display: flex;
  gap: 8px;
}
.te-btn {
  padding: 9px 22px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all .15s;
}
.te-btn-reset {
  background: transparent;
  border-color: #d8d9d4;
  color: #858982;
}
.te-btn-reset:hover {
  background: #f0f1ed;
  color: #5c605a;
}
.te-btn-cancel {
  background: #fff;
  border-color: #d8d9d4;
  color: #5c605a;
}
.te-btn-cancel:hover {
  background: #f0f1ed;
}
.te-btn-insert {
  background: #4f776a;
  color: #fff;
  border-color: #4f776a;
}
.te-btn-insert:hover {
  background: #3e5f54;
  border-color: #3e5f54;
}

/* ---- 响应式 ---- */
@media (max-width: 640px) {
  .te-dialog { max-height: 95vh; }
  .te-templates { flex-direction: column; }
  .te-cell-input { width: 70px; }
  .te-col-letter { width: 70px; }
  .te-col-del-row .te-col-del { width: 70px; }
  .te-mode-tabs button { padding: 6px 10px; font-size: 11px; }
}
</style>
