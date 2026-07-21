/**
 * 将模板中配置的中文字体名映射为 CSS font-family 值。
 * 如果传入的字体名不在映射表中，则直接使用原名并追加通用回退。
 *
 * @param {string} chineseFontName  模板中配置的字体名，如 "宋体"、"黑体"
 * @returns {string}  CSS font-family 值，如 '"SimSun", "Songti SC", serif'
 */
export function toCssFont(chineseFontName) {
  if (!chineseFontName) return undefined

  const MAP = {
    '宋体':       '"SimSun", "Songti SC", "Noto Serif SC", serif',
    '黑体':       '"SimHei", "Heiti SC", "PingFang SC", "Microsoft YaHei", sans-serif',
    '楷体':       '"KaiTi", "Kaiti SC", "Noto Serif SC", serif',
    '楷体_GB2312': '"KaiTi_GB2312", "KaiTi", "Kaiti SC", serif',
    '仿宋':       '"FangSong", "FangSongti SC", "Noto Serif SC", serif',
    '仿宋_GB2312': '"FangSong_GB2312", "FangSong", "FangSongti SC", serif',
    '微软雅黑':     '"Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", sans-serif',
    'Times New Roman': '"Times New Roman", "Noto Serif SC", serif',
    'Arial':      'Arial, "Helvetica Neue", sans-serif',
    'Courier New': '"Courier New", "Noto Sans Mono", monospace',
  }

  // 精确匹配
  if (MAP[chineseFontName]) return MAP[chineseFontName]

  // 不区分大小写的模糊匹配
  const lower = chineseFontName.toLowerCase()
  for (const [key, value] of Object.entries(MAP)) {
    if (key.toLowerCase() === lower) return value
  }

  // 回退：直接使用原名
  return `"${chineseFontName}", serif`
}

/**
 * 从模板 formatJson 中提取字体相关的 CSS 变量对象。
 *
 * @param {object|null|undefined} formatCfg  解析后的 formatJson 对象
 * @returns {object}  { bodyFont, heading1Font, heading2Font, heading3Font, bodyFontSize, bodyLineHeight }
 */
export function extractEditorFonts(formatCfg) {
  if (!formatCfg || typeof formatCfg !== 'object') return {}

  const bodyFont      = toCssFont(formatCfg.body?.font)
  const heading1Font  = toCssFont(formatCfg.heading1?.font)
  const heading2Font  = toCssFont(formatCfg.heading2?.font)
  const heading3Font  = toCssFont(formatCfg.heading3?.font)
  const bodyFontSize    = formatCfg.body?.fontSize    || undefined
  const heading1FontSize = formatCfg.heading1?.fontSize || undefined
  const heading2FontSize = formatCfg.heading2?.fontSize || undefined
  const heading3FontSize = formatCfg.heading3?.fontSize || undefined
  const bodyLineHeight = formatCfg.body?.lineSpacing || undefined

  return {
    bodyFont, heading1Font, heading2Font, heading3Font,
    bodyFontSize, heading1FontSize, heading2FontSize, heading3FontSize,
    bodyLineHeight
  }
}
