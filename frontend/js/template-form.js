/* ===== 默认数据 ===== */
const DEFAULT_SECTIONS = [
  { key:"cover",       name:"封面",      required:true,  visible:true, editable:false, order:1  },
  { key:"declaration", name:"原创声明",  required:true,  visible:true, editable:false, order:2  },
  { key:"abstract_zh", name:"中文摘要",  required:true,  visible:true, editable:true,  order:3  },
  { key:"keywords_zh", name:"中文关键词",required:true,  visible:true, editable:true,  order:4  },
  { key:"abstract_en", name:"英文摘要",  required:true,  visible:true, editable:true,  order:5  },
  { key:"keywords_en", name:"英文关键词",required:true,  visible:true, editable:true,  order:6  },
  { key:"toc",         name:"目录",      required:true,  visible:true, editable:false, order:7  },
  { key:"chapters",    name:"正文",      required:true,  visible:true, editable:true,  order:8, multiLevel:true, maxLevel:3 },
  { key:"references",  name:"参考文献",  required:true,  visible:true, editable:true,  order:9  },
  { key:"appendix",    name:"附录",      required:false, visible:true, editable:true,  order:10 },
];

const DEFAULT_COVER_FIELDS = [
  { key:"title",           label:"论文题目",     type:"text", required:true,  maxLength:100, order:1 },
  { key:"college",         label:"学院",         type:"text", required:true,  maxLength:50,  order:2 },
  { key:"major",           label:"专业",         type:"text", required:true,  maxLength:50,  order:3 },
  { key:"studentName",     label:"学生姓名",     type:"text", required:true,  maxLength:20,  order:4 },
  { key:"studentId",       label:"学号",         type:"text", required:true,  maxLength:20,  order:5 },
  { key:"supervisor",      label:"指导老师",     type:"text", required:true,  maxLength:20,  order:6 },
  { key:"supervisorTitle", label:"指导老师职称", type:"text", required:false, maxLength:20,  order:7 },
  { key:"date",            label:"完成日期",     type:"date", required:true,  maxLength:0,   order:8 },
];

const DEFAULT_FORMAT = {
  page:       { size:"A4", marginTop:2.54, marginBottom:2.54, marginLeft:3.17, marginRight:3.17 },
  body:       { font:"宋体", fontSize:12, lineSpacing:1.5, firstLineIndent:2, alignment:"justify" },
  heading1:   { font:"黑体", fontSize:16, bold:true,  alignment:"center", spaceBefore:24, spaceAfter:18, numbering:true },
  heading2:   { font:"黑体", fontSize:14, bold:true,  alignment:"left",   spaceBefore:18, spaceAfter:12, numbering:true },
  heading3:   { font:"黑体", fontSize:12, bold:true,  alignment:"left",   spaceBefore:12, spaceAfter:6,  numbering:true },
  abstract:   { titleFont:"黑体", titleFontSize:16, titleAlignment:"center", bodyFont:"宋体", bodyFontSize:12, lineSpacing:1.5 },
  references: { font:"宋体", fontSize:10.5, lineSpacing:1.5, numberFormat:"[N]", hangingIndent:true, hangingIndentSize:2 },
  coverTitle: { font:"黑体", fontSize:18, bold:true, alignment:"center" },
  coverBody:  { font:"宋体", fontSize:14, alignment:"left", lineSpacing:2.0 },
  header:     { text:"{collegeName}本科毕业论文", font:"宋体", fontSize:9, alignment:"center", showDivider:true },
  footer:     { showPageNumber:true, pageNumberFormat:"arabic", alignment:"center", font:"Times New Roman", fontSize:9 },
};

const FORMAT_SCHEMA = [
  { group:"page", label:"页面设置", fields:[
    { key:"size",         label:"页面尺寸",       type:"select", options:["A4","A3","B5"] },
    { key:"marginTop",    label:"上边距(cm)",     type:"number", step:"0.01" },
    { key:"marginBottom", label:"下边距(cm)",     type:"number", step:"0.01" },
    { key:"marginLeft",   label:"左边距(cm)",     type:"number", step:"0.01" },
    { key:"marginRight",  label:"右边距(cm)",     type:"number", step:"0.01" },
  ]},
  { group:"body", label:"正文样式", fields:[
    { key:"font",            label:"字体",           type:"text" },
    { key:"fontSize",        label:"字号(pt)",        type:"number", step:"0.5" },
    { key:"lineSpacing",     label:"行间距",          type:"number", step:"0.1" },
    { key:"firstLineIndent", label:"首行缩进(字符)",  type:"number", step:"1" },
    { key:"alignment",       label:"对齐方式",        type:"select", options:["justify","left","center","right"] },
  ]},
  { group:"heading1", label:"一级标题", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"bold", label:"粗体", type:"checkbox" },
    { key:"alignment", label:"对齐", type:"select", options:["center","left","right"] },
    { key:"spaceBefore", label:"段前(pt)", type:"number" }, { key:"spaceAfter", label:"段后(pt)", type:"number" },
    { key:"numbering", label:"自动编号", type:"checkbox" },
  ]},
  { group:"heading2", label:"二级标题", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"bold", label:"粗体", type:"checkbox" },
    { key:"alignment", label:"对齐", type:"select", options:["left","center","right"] },
    { key:"spaceBefore", label:"段前(pt)", type:"number" }, { key:"spaceAfter", label:"段后(pt)", type:"number" },
    { key:"numbering", label:"自动编号", type:"checkbox" },
  ]},
  { group:"heading3", label:"三级标题", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"bold", label:"粗体", type:"checkbox" },
    { key:"alignment", label:"对齐", type:"select", options:["left","center","right"] },
    { key:"spaceBefore", label:"段前(pt)", type:"number" }, { key:"spaceAfter", label:"段后(pt)", type:"number" },
    { key:"numbering", label:"自动编号", type:"checkbox" },
  ]},
  { group:"abstract", label:"摘要样式", fields:[
    { key:"titleFont", label:"标题字体", type:"text" }, { key:"titleFontSize", label:"标题字号", type:"number", step:"0.5" },
    { key:"titleAlignment", label:"标题对齐", type:"select", options:["center","left"] },
    { key:"bodyFont", label:"正文字体", type:"text" }, { key:"bodyFontSize", label:"正文字号", type:"number", step:"0.5" },
    { key:"lineSpacing", label:"行间距", type:"number", step:"0.1" },
  ]},
  { group:"references", label:"参考文献", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"lineSpacing", label:"行间距", type:"number", step:"0.1" },
    { key:"numberFormat", label:"编号格式", type:"text", hint:"用[N]表示序号" },
    { key:"hangingIndent", label:"悬挂缩进", type:"checkbox" },
    { key:"hangingIndentSize", label:"缩进量(字符)", type:"number" },
  ]},
  { group:"coverTitle", label:"封面标题样式", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"bold", label:"粗体", type:"checkbox" },
    { key:"alignment", label:"对齐", type:"select", options:["center","left","right"] },
  ]},
  { group:"coverBody", label:"封面正文样式", fields:[
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"alignment", label:"对齐", type:"select", options:["left","center","justify"] },
    { key:"lineSpacing", label:"行间距", type:"number", step:"0.1" },
  ]},
  { group:"header", label:"页眉", fields:[
    { key:"text", label:"页眉文字", type:"text", hint:"支持{collegeName}占位符" },
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"alignment", label:"对齐", type:"select", options:["center","left","right"] },
    { key:"showDivider", label:"显示分隔线", type:"checkbox" },
  ]},
  { group:"footer", label:"页脚", fields:[
    { key:"showPageNumber", label:"显示页码", type:"checkbox" },
    { key:"pageNumberFormat", label:"页码格式", type:"select", options:["arabic","roman"] },
    { key:"font", label:"字体", type:"text" }, { key:"fontSize", label:"字号", type:"number", step:"0.5" },
    { key:"alignment", label:"对齐", type:"select", options:["center","left","right"] },
  ]},
];

const TemplateForm = {
  _mode: "add", _id: null, _colleges: [], _coverFields: [], _sections: [],

  async open(id) {
    this._mode = id ? "edit" : "add";
    this._id = id || null;
    this._sections = JSON.parse(JSON.stringify(DEFAULT_SECTIONS));
    this._coverFields = JSON.parse(JSON.stringify(DEFAULT_COVER_FIELDS));
    try { const cr = await api.get("/api/admin/colleges"); this._colleges = cr.data || []; } catch(e) { this._colleges = []; }
    let tpl = {}, cfg = {};
    if (id) {
      try { const r = await api.get("/api/admin/templates/" + id); tpl = r.data.template || {}; cfg = r.data.config || {}; }
      catch(e) { toast("加载模板失败", "error"); return; }
      if (cfg.structureJson) { try { this._sections = JSON.parse(cfg.structureJson).sections || this._sections; } catch(e2){} }
      if (cfg.coverFields)   { try { this._coverFields = JSON.parse(cfg.coverFields); } catch(e2){} }
    }
    Modal.open({ title: id ? ("编辑模板 · " + (tpl.name || "")) : "新增模板",
      body: this._buildBody(), confirmText: "保存", large: true, onConfirm: () => this._submit() });
    this._bindTabEvents();
    this._renderCoverFields();
    if (id) {
      const sv = (i, v) => { const e = document.getElementById(i); if (e && v != null) e.value = v; };
      sv("f-tpl-name", tpl.name); sv("f-tpl-type", tpl.type);
      sv("f-tpl-college", tpl.collegeId); sv("f-tpl-desc", tpl.description); sv("f-tpl-status", tpl.status);
      if (cfg.formatJson) { try {
        const fmt = JSON.parse(cfg.formatJson);
        FORMAT_SCHEMA.forEach(g => g.fields.forEach(fd => {
          const el = document.getElementById("fmt-" + g.group + "-" + fd.key);
          if (!el || fmt[g.group] === undefined) return;
          const v = fmt[g.group][fd.key]; if (v === undefined) return;
          if (fd.type === "checkbox") el.checked = !!v; else el.value = v;
        }));
      } catch(e2){} }
    }
  },

  _buildBody() {
    const colOpts = this._colleges.map(c => "<option value=\"" + c.id + "\">" + escHtml(c.name) + "</option>").join("");
    const tab0 = "<div class=\"form-row\">"
      + "<div class=\"form-group\"><label class=\"form-label\">模板名称<span class=\"required\">*</span></label>"
      + "<input class=\"form-control\" id=\"f-tpl-name\" placeholder=\"请输入模板名称\" maxlength=\"200\" /></div>"
      + "<div class=\"form-group\"><label class=\"form-label\">模板类型<span class=\"required\">*</span></label>"
      + "<select class=\"form-control\" id=\"f-tpl-type\"><option value=\"\">请选择</option>"
      + "<option value=\"毕业论文\">毕业论文</option><option value=\"课程论文\">课程论文</option><option value=\"项目论文\">项目论文</option>"
      + "</select></div></div>"
      + "<div class=\"form-row\">"
      + "<div class=\"form-group\"><label class=\"form-label\">所属学院<span class=\"required\">*</span></label>"
      + "<select class=\"form-control\" id=\"f-tpl-college\"><option value=\"\">请选择学院</option>" + colOpts + "</select></div>"
      + "<div class=\"form-group\"><label class=\"form-label\">状态</label>"
      + "<select class=\"form-control\" id=\"f-tpl-status\"><option value=\"0\">停用</option><option value=\"1\">启用</option></select></div></div>"
      + "<div class=\"form-group\"><label class=\"form-label\">描述</label>"
      + "<textarea class=\"form-control\" id=\"f-tpl-desc\" placeholder=\"选填\" rows=\"3\"></textarea></div>";

    const tab1 = "<div style=\"margin-bottom:10px;display:flex;justify-content:space-between;align-items:center\">"
      + "<span style=\"font-size:13px;color:var(--gray-500)\">定义论文封面的填写字段</span>"
      + "<button class=\"btn btn-sm btn-secondary\" onclick=\"TemplateForm._addCoverField()\">+ 新增字段</button></div>"
      + "<div id=\"cover-fields-list\"></div>";

    const secRows = this._sections.map(s => {
      const locked = (s.key === "cover" || s.key === "declaration" || s.key === "toc");
      return "<tr><td>" + escHtml(s.name) + "</td>"
        + "<td><input type=\"checkbox\" id=\"sec-required-" + s.key + "\"" + (s.required ? " checked" : "") + " /></td>"
        + "<td><input type=\"checkbox\" id=\"sec-visible-" + s.key + "\"" + (s.visible ? " checked" : "") + " /></td>"
        + (locked ? "<td><span class=\"section-locked\">固定</span></td>"
                  : "<td><input type=\"checkbox\" id=\"sec-editable-" + s.key + "\"" + (s.editable ? " checked" : "") + " /></td>")
        + (s.key === "chapters"
          ? "<td><input type=\"number\" id=\"sec-maxlevel-chapters\" value=\"" + (s.maxLevel || 3) + "\" min=\"1\" max=\"5\" style=\"width:50px;padding:4px;border:1px solid var(--gray-300);border-radius:4px;font-size:13px\" /></td>"
          : "<td></td>")
        + "</tr>";
    }).join("");

    const tab2 = "<table class=\"section-table\" style=\"width:100%\">"
      + "<thead><tr><th style=\"width:130px\">章节名称</th><th>必填</th><th>显示</th><th>可编辑</th><th style=\"width:100px\">最大层级</th></tr></thead>"
      + "<tbody>" + secRows + "</tbody></table>";

    const fmtGroups = FORMAT_SCHEMA.map(g => {
      const gDef = DEFAULT_FORMAT[g.group] || {};
      const fields = g.fields.map(fd => {
        const defVal = gDef[fd.key];
        const eid = "fmt-" + g.group + "-" + fd.key;
        let inp = "";
        if (fd.type === "checkbox") {
          inp = "<input type=\"checkbox\" id=\"" + eid + "\"" + (defVal ? " checked" : "") + " />";
        } else if (fd.type === "select") {
          const opts = fd.options.map(o => "<option value=\"" + o + "\"" + (defVal === o ? " selected" : "") + ">" + o + "</option>").join("");
          inp = "<select class=\"form-control\" id=\"" + eid + "\">" + opts + "</select>";
        } else {
          const sv = fd.step ? (" step=\"" + fd.step + "\"") : "";
          inp = "<input type=\"" + fd.type + "\" class=\"form-control\" id=\"" + eid + "\" value=\"" + (defVal !== undefined ? defVal : "") + "\"" + sv + " />";
        }
        const hint = fd.hint ? ("<div class=\"form-hint\">" + escHtml(fd.hint) + "</div>") : "";
        return "<div class=\"fmt-field\"><label>" + escHtml(fd.label) + "</label>" + inp + hint + "</div>";
      }).join("");
      return "<div class=\"fmt-group\"><div class=\"fmt-group-title\">" + escHtml(g.label) + "</div><div class=\"fmt-grid\">" + fields + "</div></div>";
    }).join("");

    return "<div class=\"tab-bar\" id=\"tpl-tab-bar\">"
      + "<button class=\"tab-btn active\" data-tab=\"0\">基本信息</button>"
      + "<button class=\"tab-btn\" data-tab=\"1\">封面字段</button>"
      + "<button class=\"tab-btn\" data-tab=\"2\">章节结构</button>"
      + "<button class=\"tab-btn\" data-tab=\"3\">格式参数</button></div>"
      + "<div class=\"tab-panel active\" data-panel=\"0\">" + tab0 + "</div>"
      + "<div class=\"tab-panel\" data-panel=\"1\">" + tab1 + "</div>"
      + "<div class=\"tab-panel\" data-panel=\"2\">" + tab2 + "</div>"
      + "<div class=\"tab-panel\" data-panel=\"3\">" + fmtGroups + "</div>";
  },

  _bindTabEvents() {
    document.querySelectorAll("#tpl-tab-bar .tab-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        if (btn.dataset.tab === "1") this._syncCoverFromDom();
        const idx = btn.dataset.tab;
        document.querySelectorAll("#tpl-tab-bar .tab-btn").forEach(b => b.classList.remove("active"));
        document.querySelectorAll("#modal-body .tab-panel").forEach(p => p.classList.remove("active"));
        btn.classList.add("active");
        const panel = document.querySelector("#modal-body .tab-panel[data-panel=\"" + idx + "\"]");
        if (panel) panel.classList.add("active");
      });
    });
  },

  _renderCoverFields() {
    const c = document.getElementById("cover-fields-list"); if (!c) return;
    c.innerHTML = this._coverFields.map((f, i) => {
      const checked = f.required ? " checked" : "";
      return "<div class=\"cover-field-row\" data-idx=\"" + i + "\">"
        + "<div class=\"cf-order-badge\">" + (i + 1) + "</div>"
        + "<input class=\"form-control cf-label\" placeholder=\"标签名\" value=\"" + escHtml(f.label) + "\" data-field=\"label\" />"
        + "<input class=\"form-control cf-key\" placeholder=\"Key(英文)\" value=\"" + escHtml(f.key) + "\" data-field=\"key\" />"
        + "<select class=\"form-control cf-type\" data-field=\"type\">"
        + "<option value=\"text\"" + (f.type === "text" ? " selected" : "") + ">文本</option>"
        + "<option value=\"date\"" + (f.type === "date" ? " selected" : "") + ">日期</option></select>"
        + "<input class=\"form-control cf-maxlen\" type=\"number\" value=\"" + (f.maxLength || "") + "\" data-field=\"maxLength\" />"
        + "<div class=\"cf-req\"><input type=\"checkbox\"" + checked + " data-field=\"required\" title=\"必填\" /></div>"
        + "<div class=\"cf-actions\">"
        + "<button class=\"btn-icon\" onclick=\"TemplateForm._moveCover(" + i + ",-1)\" title=\"上移\">&#8593;</button>"
        + "<button class=\"btn-icon\" onclick=\"TemplateForm._moveCover(" + i + ",1)\" title=\"下移\">&#8595;</button>"
        + "<button class=\"btn-icon\" onclick=\"TemplateForm._removeCover(" + i + ")\" title=\"删除\" style=\"color:var(--danger)\">&#10005;</button>"
        + "</div></div>";
    }).join("");
  },

  _syncCoverFromDom() {
    document.querySelectorAll("#cover-fields-list .cover-field-row").forEach((row, i) => {
      if (!this._coverFields[i]) return;
      row.querySelectorAll("[data-field]").forEach(el => {
        const field = el.dataset.field;
        if (el.type === "checkbox") this._coverFields[i][field] = el.checked;
        else if (field === "maxLength") this._coverFields[i][field] = parseInt(el.value) || 0;
        else this._coverFields[i][field] = el.value;
      });
    });
  },

  _addCoverField() {
    this._syncCoverFromDom();
    this._coverFields.push({ key: "", label: "新字段", type: "text", required: false, maxLength: 50, order: this._coverFields.length + 1 });
    this._renderCoverFields();
  },

  _moveCover(idx, dir) {
    this._syncCoverFromDom();
    const t = idx + dir;
    if (t < 0 || t >= this._coverFields.length) return;
    [this._coverFields[idx], this._coverFields[t]] = [this._coverFields[t], this._coverFields[idx]];
    this._coverFields.forEach((f, i) => f.order = i + 1);
    this._renderCoverFields();
  },

  _removeCover(idx) {
    this._syncCoverFromDom();
    this._coverFields.splice(idx, 1);
    this._coverFields.forEach((f, i) => f.order = i + 1);
    this._renderCoverFields();
  },

  async _submit() {
    const name  = (document.getElementById("f-tpl-name")?.value || "").trim();
    const type  = (document.getElementById("f-tpl-type")?.value || "").trim();
    const cid   = (document.getElementById("f-tpl-college")?.value || "").trim();
    const desc  = (document.getElementById("f-tpl-desc")?.value || "").trim();
    const status = parseInt(document.getElementById("f-tpl-status")?.value || "0");
    if (!name) { toast("模板名称不能为空", "warning"); return; }
    if (!type) { toast("请选择模板类型",   "warning"); return; }
    if (!cid)  { toast("请选择所属学院",   "warning"); return; }
    const sections = this._sections.map(s => {
      const locked = (s.key === "cover" || s.key === "declaration" || s.key === "toc");
      const sec = Object.assign({}, s, {
        required: document.getElementById("sec-required-" + s.key)?.checked ?? s.required,
        visible:  document.getElementById("sec-visible-"  + s.key)?.checked ?? s.visible,
        editable: locked ? false : (document.getElementById("sec-editable-" + s.key)?.checked ?? s.editable),
      });
      if (s.key === "chapters") sec.maxLevel = parseInt(document.getElementById("sec-maxlevel-chapters")?.value || "3");
      return sec;
    });
    this._syncCoverFromDom();
    const format = {};
    FORMAT_SCHEMA.forEach(g => {
      format[g.group] = Object.assign({}, DEFAULT_FORMAT[g.group] || {});
      g.fields.forEach(fd => {
        const el = document.getElementById("fmt-" + g.group + "-" + fd.key); if (!el) return;
        if (fd.type === "checkbox") format[g.group][fd.key] = el.checked;
        else if (fd.type === "number") format[g.group][fd.key] = parseFloat(el.value) || 0;
        else format[g.group][fd.key] = el.value;
      });
    });
    const payload = {
      name, type, collegeId: parseInt(cid), description: desc, status,
      structureJson: JSON.stringify({ sections }),
      formatJson:    JSON.stringify(format),
      coverFields:   JSON.stringify(this._coverFields),
    };
    try {
      const res = this._mode === "add"
        ? await api.post("/api/admin/templates", payload)
        : await api.put("/api/admin/templates/" + this._id, payload);
      if (res.code === 200) {
        toast(this._mode === "add" ? "新增成功" : "修改成功", "success");
        Modal.close();
        PageTemplates._load();
      } else { toast(res.message || "保存失败", "error"); }
    } catch(e) { toast(e.message || "请求失败", "error"); }
  },
};

/* ===== TemplateDetail ===== */
const TemplateDetail = {
  async open(id) {
    let tpl = {}, cfg = {};
    try {
      const r = await api.get("/api/admin/templates/" + id);
      tpl = r.data.template || {}; cfg = r.data.config || {};
    } catch(e) { toast("加载失败", "error"); return; }

    const collegeName = (PageTemplates._colleges.find(c => c.id === tpl.collegeId) || {}).name || tpl.collegeId;
    const statusBadge = tpl.status === 1
      ? "<span class=\"badge badge-success\">已启用</span>"
      : "<span class=\"badge badge-gray\">已停用</span>";

    let secHtml = "", covHtml = "", fmtHtml = "";

    if (cfg.structureJson) {
      try {
        const secs = JSON.parse(cfg.structureJson).sections || [];
        const yn = v => v ? "<span style=\"color:var(--success)\">是</span>" : "<span style=\"color:var(--gray-400)\">否</span>";
        secHtml = "<table style=\"width:100%\"><thead><tr><th>章节名称</th><th>必填</th><th>显示</th><th>可编辑</th></tr></thead><tbody>"
          + secs.map(s => "<tr><td>" + escHtml(s.name) + "</td><td>" + yn(s.required) + "</td><td>" + yn(s.visible) + "</td><td>" + yn(s.editable) + "</td></tr>").join("") + "</tbody></table>";
      } catch(e2) { secHtml = "解析失败"; }
    }

    if (cfg.coverFields) {
      try {
        const fields = JSON.parse(cfg.coverFields);
        covHtml = "<table style=\"width:100%\"><thead><tr><th>序</th><th>标签名</th><th>Key</th><th>类型</th><th>必填</th><th>最大长度</th></tr></thead><tbody>"
          + fields.map(f => "<tr><td>" + f.order + "</td><td>" + escHtml(f.label) + "</td><td><code>" + escHtml(f.key) + "</code></td>"
            + "<td>" + f.type + "</td><td>" + (f.required ? "是" : "否") + "</td><td>" + (f.maxLength || "-") + "</td></tr>").join("") + "</tbody></table>";
      } catch(e2) { covHtml = "解析失败"; }
    }

    if (cfg.formatJson) {
      try {
        const fmt = JSON.parse(cfg.formatJson);
        fmtHtml = Object.keys(fmt).map(gk => {
          const gDef = FORMAT_SCHEMA.find(g => g.group === gk);
          const label = gDef ? gDef.label : gk;
          const kvs = Object.entries(fmt[gk]).map(([k,v]) => {
            const fd = gDef ? gDef.fields.find(f => f.key === k) : null;
            return "<div class=\"detail-k\">" + escHtml(fd ? fd.label : k) + "</div><div class=\"detail-v\">" + escHtml(String(v)) + "</div>";
          }).join("");
          return "<div class=\"detail-section\"><div class=\"detail-section-title\">" + escHtml(label) + "</div><div class=\"detail-kv\">" + kvs + "</div></div>";
        }).join("");
      } catch(e2) { fmtHtml = "解析失败"; }
    }

    const body = "<div class=\"detail-section\"><div class=\"detail-section-title\">基本信息</div>"
      + "<div class=\"detail-kv\">"
      + "<div class=\"detail-k\">模板名称</div><div class=\"detail-v\">" + escHtml(tpl.name) + "</div>"
      + "<div class=\"detail-k\">模板类型</div><div class=\"detail-v\">" + escHtml(tpl.type) + "</div>"
      + "<div class=\"detail-k\">所属学院</div><div class=\"detail-v\">" + escHtml(collegeName) + "</div>"
      + "<div class=\"detail-k\">状态</div><div class=\"detail-v\">" + statusBadge + "</div>"
      + "<div class=\"detail-k\">版本</div><div class=\"detail-v\">v" + (tpl.version || 1) + "</div>"
      + "</div></div>"
      + (secHtml ? "<div class=\"detail-section\"><div class=\"detail-section-title\">章节结构</div>" + secHtml + "</div>" : "")
      + (covHtml ? "<div class=\"detail-section\"><div class=\"detail-section-title\">封面字段</div>" + covHtml + "</div>" : "")
      + (fmtHtml ? "<div class=\"detail-section\"><div class=\"detail-section-title\">格式参数</div>" + fmtHtml + "</div>" : "");

    Modal.open({ title: "模板详情 · " + escHtml(tpl.name), body, large: true, onConfirm: null });
    document.getElementById("modal-footer").innerHTML = "<button class=\"btn btn-secondary\" id=\"modal-cancel\">关闭</button>";
    document.getElementById("modal-cancel").addEventListener("click", () => Modal.close());
  },
};
