/* ===== 路由 ===== */
function navigate(path) {
  location.hash = path;
}

function handleRoute() {
  const path = location.hash.replace('#', '') || '/colleges';
  setActiveNav(path);
  if (path === '/colleges')  { PageColleges.render();  return; }
  if (path === '/templates') { PageTemplates.render(); return; }
  navigate('/colleges');
}

function setActiveNav(path) {
  document.querySelectorAll('.nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.route === path);
  });
  const labels = {
    '/colleges': '学院管理',
    '/templates': '模板管理',
  };
  document.getElementById('breadcrumb-current').textContent = labels[path] || '';
}

/* ===== Toast ===== */
function toast(message, type = 'default') {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = 'toast' + (type !== 'default' ? ' toast-' + type : '');
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 2800);
}

/* ===== Modal ===== */
const Modal = {
  _confirmCb: null,

  open({ title, body, confirmText = '确定', onConfirm, large = false }) {
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-body').innerHTML = body;
    document.getElementById('modal-confirm').textContent = confirmText;
    document.getElementById('modal').classList.toggle('modal-lg', large);
    document.getElementById('modal-overlay').classList.add('open');
    this._confirmCb = onConfirm || null;
  },

  close() {
    document.getElementById('modal-overlay').classList.remove('open');
    this._confirmCb = null;
  },

  confirm() {
    if (this._confirmCb) this._confirmCb();
  },
};

document.getElementById('modal-close').addEventListener('click',  () => Modal.close());
document.getElementById('modal-cancel').addEventListener('click', () => Modal.close());
document.getElementById('modal-confirm').addEventListener('click', () => Modal.confirm());
document.getElementById('modal-overlay').addEventListener('click', e => {
  if (e.target === document.getElementById('modal-overlay')) Modal.close();
});

/* ===== 工具函数 ===== */
function escHtml(s) {
  return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function fmtDate(str) {
  if (!str) return '-';
  return str.replace('T', ' ').slice(0, 16);
}

/* ===================================================
   页面：学院管理
=================================================== */
const PageColleges = {
  _data: [],

  render() {
    document.getElementById('page-content').innerHTML = `
      <div class="card">
        <div class="card-header">
          <span class="card-title">学院列表</span>
          <div class="card-actions">
            <div class="search-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input class="search-input" id="college-search" placeholder="搜索学院名称" />
            </div>
            <button class="btn btn-primary" id="btn-add-college">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              新增学院
            </button>
          </div>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width:60px">ID</th>
                <th>学院名称</th>
                <th>描述</th>
                <th>创建时间</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody id="college-tbody"></tbody>
          </table>
        </div>
      </div>`;
    this._bindEvents();
    this._load();
  },

  _renderRows(list) {
    const tbody = document.getElementById('college-tbody');
    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="5">
        <div class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <p>暂无学院数据</p>
        </div>
      </td></tr>`;
      return;
    }
    tbody.innerHTML = list.map(c => `
      <tr>
        <td>${escHtml(c.id)}</td>
        <td><strong>${escHtml(c.name)}</strong></td>
        <td style="color:var(--text-mute)">${escHtml(c.description || '-')}</td>
        <td style="color:var(--text-dim);font-size:13px">${fmtDate(c.createTime)}</td>
        <td class="col-actions">
          <button class="btn btn-sm btn-secondary" onclick="PageColleges._openEdit(${c.id})">编辑</button>
          <button class="btn btn-sm btn-danger" onclick="PageColleges._confirmDelete(${c.id}, '${escHtml(c.name)}')">删除</button>
        </td>
      </tr>`).join('');
  },

  async _load(keyword = '') {
    try {
      const res = await api.get('/api/admin/colleges' + (keyword ? '?keyword=' + encodeURIComponent(keyword) : ''));
      this._data = res.data || [];
      this._renderRows(this._data);
    } catch (e) {
      toast(e.message || '加载失败', 'error');
    }
  },

  _formHtml(college = {}) {
    return `
      <div class="form-group">
        <label class="form-label">学院名称<span class="required">*</span></label>
        <input class="form-control" id="f-college-name" placeholder="请输入学院名称" maxlength="100"
               value="${escHtml(college.name || '')}" />
      </div>
      <div class="form-group">
        <label class="form-label">描述</label>
        <textarea class="form-control" id="f-college-desc" placeholder="选填，简要说明该学院"
                  rows="3">${escHtml(college.description || '')}</textarea>
      </div>`;
  },

  _openAdd() {
    Modal.open({
      title: '新增学院',
      body: this._formHtml(),
      confirmText: '保存',
      onConfirm: () => this._submitAdd(),
    });
  },

  async _submitAdd() {
    const name = document.getElementById('f-college-name').value.trim();
    const description = document.getElementById('f-college-desc').value.trim();
    if (!name) { toast('学院名称不能为空', 'warning'); return; }
    try {
      const res = await api.post('/api/admin/colleges', { name, description });
      if (res.code === 200) {
        toast('新增成功', 'success');
        Modal.close();
        this._load();
      } else {
        toast(res.message || '新增失败', 'error');
      }
    } catch (e) {
      toast(e.message || '请求失败', 'error');
    }
  },

  async _openEdit(id) {
    const college = this._data.find(c => c.id === id);
    if (!college) return;
    Modal.open({
      title: '编辑学院',
      body: this._formHtml(college),
      confirmText: '保存',
      onConfirm: () => this._submitEdit(id),
    });
  },

  async _submitEdit(id) {
    const name = document.getElementById('f-college-name').value.trim();
    const description = document.getElementById('f-college-desc').value.trim();
    if (!name) { toast('学院名称不能为空', 'warning'); return; }
    try {
      const res = await api.put('/api/admin/colleges/' + id, { name, description });
      if (res.code === 200) {
        toast('修改成功', 'success');
        Modal.close();
        this._load();
      } else {
        toast(res.message || '修改失败', 'error');
      }
    } catch (e) {
      toast(e.message || '请求失败', 'error');
    }
  },

  _confirmDelete(id, name) {
    Modal.open({
      title: '确认删除',
      body: `<p>确定要删除学院「<strong>${escHtml(name)}</strong>」吗？<br/>
             <span style="color:var(--text-dim);font-size:13px">删除后无法恢复，请确认该学院下没有关联模板。</span></p>`,
      confirmText: '删除',
      onConfirm: () => this._doDelete(id),
    });
    document.getElementById('modal-confirm').style.cssText =
      'background:var(--danger);border-color:var(--danger);color:#fff';
  },

  async _doDelete(id) {
    try {
      const res = await api.delete('/api/admin/colleges/' + id);
      if (res.code === 200) {
        toast('删除成功', 'success');
        Modal.close();
        this._load();
      } else {
        toast(res.message || '删除失败', 'error');
      }
    } catch (e) {
      toast(e.message || '请求失败', 'error');
    }
  },

  _bindEvents() {
    document.getElementById('btn-add-college').addEventListener('click', () => this._openAdd());
    let timer;
    document.getElementById('college-search').addEventListener('input', e => {
      clearTimeout(timer);
      timer = setTimeout(() => this._load(e.target.value.trim()), 300);
    });
  },
};

/* ===================================================
   页面：模板管理
=================================================== */
const PageTemplates = {
  _data: [],
  _colleges: [],

  render() {
    document.getElementById('page-content').innerHTML = `
      <div class="card">
        <div class="card-header">
          <span class="card-title">模板列表</span>
          <div class="card-actions">
            <div class="search-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input class="search-input" id="tpl-search" placeholder="搜索模板名称" />
            </div>
            <button class="btn btn-primary" id="btn-add-tpl">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              新增模板
            </button>
          </div>
        </div>

        <div class="filter-bar">
          <label>学院</label>
          <select class="filter-select" id="tpl-filter-college">
            <option value="">全部</option>
          </select>
          <label>类型</label>
          <select class="filter-select" id="tpl-filter-type">
            <option value="">全部</option>
            <option value="毕业论文">毕业论文</option>
            <option value="课程论文">课程论文</option>
            <option value="项目论文">项目论文</option>
          </select>
          <label>状态</label>
          <select class="filter-select" id="tpl-filter-status">
            <option value="">全部</option>
            <option value="1">已启用</option>
            <option value="0">已停用</option>
          </select>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width:60px">ID</th>
                <th>模板名称</th>
                <th>类型</th>
                <th>所属学院</th>
                <th>状态</th>
                <th>版本</th>
                <th>更新时间</th>
                <th class="col-actions">操作</th>
              </tr>
            </thead>
            <tbody id="tpl-tbody"></tbody>
          </table>
        </div>
      </div>`;
    this._bindEvents();
    this._loadColleges().then(() => this._load());
  },

  async _loadColleges() {
    try {
      const res = await api.get('/api/admin/colleges');
      this._colleges = res.data || [];
      const sel = document.getElementById('tpl-filter-college');
      if (!sel) return;
      this._colleges.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = c.name;
        sel.appendChild(opt);
      });
    } catch (_) { /* 学院加载失败不阻塞模板列表 */ }
  },

  _collegeName(id) {
    const c = this._colleges.find(c => c.id === id);
    return c ? c.name : id;
  },

  _statusBadge(status) {
    return status === 1
      ? '<span class="badge badge-success">已启用</span>'
      : '<span class="badge badge-gray">已停用</span>';
  },

  _renderRows(list) {
    const tbody = document.getElementById('tpl-tbody');
    if (!tbody) return;
    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="8">
        <div class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/>
          </svg>
          <p>暂无模板数据</p>
        </div>
      </td></tr>`;
      return;
    }
    tbody.innerHTML = list.map(t => `
      <tr>
        <td>${escHtml(t.id)}</td>
        <td><strong>${escHtml(t.name)}</strong></td>
        <td><span class="badge badge-primary">${escHtml(t.type)}</span></td>
        <td>${escHtml(this._collegeName(t.collegeId))}</td>
        <td>${this._statusBadge(t.status)}</td>
        <td style="color:var(--text-dim)">v${escHtml(t.version)}</td>
        <td style="color:var(--text-dim);font-size:13px">${fmtDate(t.updateTime)}</td>
        <td class="col-actions">
          <button class="btn btn-sm btn-secondary" onclick="PageTemplates._openDetail(${t.id})">详情</button>
          <button class="btn btn-sm btn-secondary" onclick="PageTemplates._openEdit(${t.id})">编辑</button>
          <button class="btn btn-sm ${t.status===1?'btn-danger':'btn-primary'}"
                  onclick="PageTemplates._toggleStatus(${t.id}, ${t.status})">
            ${t.status === 1 ? '停用' : '启用'}
          </button>
          <button class="btn btn-sm btn-danger" onclick="PageTemplates._confirmDelete(${t.id}, '${escHtml(t.name)}')">删除</button>
        </td>
      </tr>`).join('');
  },

  async _load() {
    const collegeId = document.getElementById('tpl-filter-college')?.value || '';
    const type      = document.getElementById('tpl-filter-type')?.value    || '';
    const status    = document.getElementById('tpl-filter-status')?.value  || '';
    const params = new URLSearchParams();
    if (collegeId) params.set('collegeId', collegeId);
    if (type)      params.set('type', type);
    if (status !== '') params.set('status', status);
    const qs = params.toString();
    try {
      const res = await api.get('/api/admin/templates' + (qs ? '?' + qs : ''));
      this._data = res.data || [];
      const keyword = document.getElementById('tpl-search')?.value.trim().toLowerCase() || '';
      const filtered = keyword
        ? this._data.filter(t => t.name.toLowerCase().includes(keyword))
        : this._data;
      this._renderRows(filtered);
    } catch (e) {
      toast(e.message || '加载失败', 'error');
    }
  },

  _openDetail(id) {
    TemplateDetail.open(id);
  },

  _openEdit(id) {
    TemplateForm.open(id);
  },

  _openAdd() {
    TemplateForm.open(null);
  },

  async _toggleStatus(id, currentStatus) {
    const next = currentStatus === 1 ? 0 : 1;
    try {
      const res = await api.put('/api/admin/templates/' + id + '/status?status=' + next);
      if (res.code === 200) {
        toast(res.message || '操作成功', 'success');
        this._load();
      } else {
        toast(res.message || '操作失败', 'error');
      }
    } catch (e) {
      toast(e.message || '请求失败', 'error');
    }
  },

  _confirmDelete(id, name) {
    Modal.open({
      title: '确认删除',
      body: `<p>确定要删除模板「<strong>${escHtml(name)}</strong>」吗？<br/>
             <span style="color:var(--text-dim);font-size:13px">删除后不可恢复，已使用该模板的论文不受影响。</span></p>`,
      confirmText: '删除',
      onConfirm: () => this._doDelete(id),
    });
    document.getElementById('modal-confirm').style.cssText =
      'background:var(--danger);border-color:var(--danger);color:#fff';
  },

  async _doDelete(id) {
    try {
      const res = await api.delete('/api/admin/templates/' + id);
      if (res.code === 200) {
        toast('删除成功', 'success');
        Modal.close();
        this._load();
      } else {
        toast(res.message || '删除失败', 'error');
      }
    } catch (e) {
      toast(e.message || '请求失败', 'error');
    }
  },

  _bindEvents() {
    document.getElementById('btn-add-tpl').addEventListener('click', () => this._openAdd());
    ['tpl-filter-college', 'tpl-filter-type', 'tpl-filter-status'].forEach(id => {
      document.getElementById(id)?.addEventListener('change', () => this._load());
    });
    let timer;
    document.getElementById('tpl-search').addEventListener('input', e => {
      clearTimeout(timer);
      timer = setTimeout(() => this._load(), 300);
    });
  },
};

/* ===== 退出登录 ===== */
document.getElementById('btn-logout').addEventListener('click', () => {
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
  // 跳转到登录页（路径按实际情况调整）
  location.href = '../login.html';
});

/* ===== 回填用户信息 ===== */
function initUserInfo() {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}');
    if (info.nickname) {
      const nameEl = document.getElementById('sidebar-username');
      if (nameEl) nameEl.textContent = info.nickname;
    }
  } catch (_) {}
}

/* ===== 启动 ===== */
window.addEventListener('hashchange', handleRoute);
initUserInfo();
handleRoute();
