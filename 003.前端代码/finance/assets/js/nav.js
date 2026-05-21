/* ========================================================================
   nav.js — 底部导航栏共享脚本（每日财务管家原型）
   使用方式：
     1. 在页面 <body> 末尾添加占位符：<div id="bottom-nav"></div>
     2. 引入本脚本：<script src="../assets/js/nav.js"></script>
     3. 初始化并标记当前页面：<script>Nav.init('home');</script>
        可选值：'home' | 'statistics' | 'record' | 'settings'
   ======================================================================== */

var Nav = (function () {
    'use strict';

    /* 页面路由映射：key 对应 Nav.init() 参数，value 为相对路径 */
    var PAGES = {
        home: 'home.html',
        statistics: 'statistics.html',
        record: 'record.html',
        settings: 'settings.html'
    };

    /* 底部导航栏标签定义 */
    var TABS = [
        { key: 'home', icon: 'home', label: '首页' },
        { key: 'statistics', icon: 'insights', label: '统计' },
        { key: 'record', icon: 'add_circle', label: '记账' },
        { key: 'settings', icon: 'person', label: '我的' }
    ];

    /* --------------------------------------------------------------------
       render(activeTab) — 渲染底部导航栏到 <div id="bottom-nav">
       @param {string} activeTab — 当前激活的标签 key（如 'home'）
       注意：'record' 标签被跳过，因为记账入口由首页 FAB 按钮提供
    -------------------------------------------------------------------- */
    function render(activeTab) {
        var nav = document.getElementById('bottom-nav');
        if (!nav) return;

        /* 让 #bottom-nav 本身成为 flex 容器，居中排列 */
        nav.className += ' flex items-center justify-center w-full gap-[350px]';

        var html = '';

        for (var i = 0; i < TABS.length; i++) {
            var tab = TABS[i];

            /* 记账标签不渲染到底部导航 — 由 FAB 按钮替代 */
            if (tab.key === 'record') continue;

            var isActive = activeTab === tab.key;
            var iconFill = isActive ? ' style="font-variation-settings: \'FILL\' 1;"' : '';

            if (isActive) {
                html += '<a class="flex flex-col items-center justify-center px-5 py-1.5 bg-secondary-container text-on-secondary-container rounded-2xl transition-all" href="' + PAGES[tab.key] + '">';
            } else {
                html += '<a class="flex flex-col items-center justify-center px-5 py-1.5 text-on-surface-variant hover:text-primary transition-all" href="' + PAGES[tab.key] + '">';
            }
            html += '<span class="material-symbols-outlined"' + iconFill + '>' + tab.icon + '</span>';
            html += '<span class="text-xs mt-0.5">' + tab.label + '</span>';
            html += '</a>';
        }

        nav.innerHTML = html;
    }

    /* 暴露公共 API：init 用于初始化，pages 为路由映射表（供外部参考） */
    return { init: render, pages: PAGES };
})();
