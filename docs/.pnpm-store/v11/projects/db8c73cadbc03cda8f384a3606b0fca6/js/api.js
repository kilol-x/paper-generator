/* ===== API 请求工具 ===== */

const BASE_URL = 'http://localhost:8080';

const api = (() => {
  function getToken() {
    return localStorage.getItem('token') || '';
  }

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const options = { method, headers };
    if (body !== undefined) options.body = JSON.stringify(body);

    const res = await fetch(BASE_URL + path, options);
    const data = await res.json().catch(() => ({ code: res.status, message: '响应解析失败' }));

    if (data.code === 401 || data.code === 403) {
      // token 失效时跳回登录（由外层处理，这里只抛出）
      const err = new Error(data.message || '未授权');
      err.code = data.code;
      throw err;
    }
    return data;
  }

  return {
    get:    (path)        => request('GET',    path),
    post:   (path, body)  => request('POST',   path, body),
    put:    (path, body)  => request('PUT',    path, body),
    patch:  (path, body)  => request('PATCH',  path, body),
    delete: (path)        => request('DELETE', path),
  };
})();
