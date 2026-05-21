/* ========================================================================
   auth.js — 前端通用鉴权工具（所有需要登录的页面引用）
   使用方式：
     1. 在页面 <head> 中引入：<script src="../assets/js/auth.js"></script>
     2. 调用 Auth.check() 验证登录状态，未登录则跳转 index.html
   ======================================================================== */

// ==================== 后端切换 ====================
// Node.js 后端: var API_BASE = 'http://localhost:3000';
var API_BASE = 'http://localhost:8080';
// ================================================

var Auth = (function () {
    'use strict';

    function getToken() {
        return localStorage.getItem('token') || '';
    }

    function setToken(token) {
        localStorage.setItem('token', token);
    }

    function clearToken() {
        localStorage.removeItem('token');
    }

    /**
     * 检查登录状态，未登录则跳转登录页
     * @param {boolean} redirect - 是否自动跳转（默认 true）
     * @returns {string} token
     */
    function check(redirect) {
        var token = getToken();
        if (!token) {
            if (redirect !== false) {
                window.location.replace('../index.html');
            }
            return '';
        }
        return token;
    }

    /**
     * 退出登录
     */
    function logout() {
        clearToken();
        window.location.href = '../index.html';
    }

    /**
     * 发起带认证的 fetch 请求
     * @param {string} url - 请求路径（相对于 /api/）
     * @param {object} options - fetch 选项
     * @returns {Promise} 解析后的 JSON 数据
     */
    function fetchApi(url, options) {
        options = options || {};
        var token = check();
        var headers = options.headers || {};
        headers['Authorization'] = 'Bearer ' + token;
        if (!headers['Content-Type'] && options.body && !(options.body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
        }
        return fetch(API_BASE + '/api' + url, Object.assign({}, options, { headers: headers }))
            .then(function (res) {
                return res.json().then(function (data) {
                    if (res.status === 401) {
                        // token 失效，跳转登录页
                        clearToken();
                        window.location.href = '../index.html';
                        return Promise.reject(new Error('未登录'));
                    }
                    return data;
                });
            });
    }

    return {
        getToken: getToken,
        setToken: setToken,
        clearToken: clearToken,
        check: check,
        logout: logout,
        fetchApi: fetchApi
    };
})();
