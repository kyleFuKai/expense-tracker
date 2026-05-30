# 二级分类 (V1.5) 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在记账页和类别管理页增加两级分类支持——父分类 Tab + 子分类网格，以及创建子分类的能力。

**Architecture:** 后端不需要修改（数据库 `parent_id`、Java 实体、DTO、Service 均已就绪）。只修改前端两个页面：
1. `record.html` — 按 `parent_id` 分组分类，渲染为顶部 Tab + 子分类 4 列网格
2. `category-manage.html` — 添加分类弹窗增加"上级分类"选择器，分类展示支持层级缩进

**Tech Stack:** HTML5, Tailwind CSS (CDN), Vanilla JS (IIFE), MyBatis-Plus 后端已就绪

---

## 文件结构总览

### 修改文件
| 文件 | 变更 |
|------|------|
| `003.前端代码/finance/pages/record.html` | 重写 `loadCategories()` 函数：按 `parent_id` 分组，添加顶部 Tab + 子分类网格渲染逻辑 |
| `003.前端代码/finance/pages/category-manage.html` | 添加弹窗"上级分类"选择器，分类展示按层级缩进 |

---

### Task 1: record.html — 顶部 Tab + 子分类网格

**Files:**
- Modify: `003.前端代码/finance/pages/record.html`

- [ ] **Step 1: 添加顶部 Tab 区域 HTML**

在现有 `<div id="category-grid" ...>` 上方（即分类选择网格容器之前），添加一个新的父分类 Tab 容器。将现有的分类区域包裹结构改为：

```html
            <!-- 父分类 Tab（横向滚动） -->
            <div id="category-tabs" class="flex gap-2 overflow-x-auto pb-2 no-scrollbar" style="scrollbar-width: none; -ms-overflow-style: none;">
            </div>

            <!-- 子分类选择网格（动态渲染） -->
            <div id="category-grid" class="grid grid-cols-4 gap-y-6 pt-2">
                <div class="col-span-4 flex items-center justify-center py-8 text-on-surface-variant">
                    <span class="material-symbols-outlined animate-spin mr-2">progress_activity</span>
                    加载分类中...
                </div>
            </div>
```

在 `<style>` 块中添加 Tab 样式：

```css
        .parent-tab {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 6px 14px;
            border-radius: 14px;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            white-space: nowrap;
            border: 1px solid #e2e8f0;
            background: #f1f5f9;
            color: #64748b;
            transition: all 0.15s;
            flex-shrink: 0;
        }
        .parent-tab.active {
            background: #E8F0FE;
            color: #004ac6;
            border-color: #004ac6;
            font-weight: 600;
        }
        .parent-tab .tab-icon {
            font-size: 16px;
        }
        .child-btn {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 6px;
            padding: 8px 4px;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.15s;
            border: 2px solid transparent;
        }
        .child-btn:hover {
            background: #f8f9ff;
        }
        .child-btn.selected {
            border-color: #004ac6;
            background: #E8F0FE;
        }
        .child-btn .child-icon {
            width: 44px;
            height: 44px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            background: #f1f5f9;
            transition: all 0.15s;
        }
        .child-btn.selected .child-icon {
            background: #dbeafe;
        }
        .child-label {
            font-size: 11px;
            color: #64748b;
            text-align: center;
            line-height: 1.2;
        }
        .child-btn.selected .child-label {
            color: #004ac6;
            font-weight: 600;
        }
        .empty-sub-msg {
            text-align: center;
            color: #94a3b8;
            font-size: 13px;
            padding: 24px 0;
            grid-column: 1 / -1;
        }
```

- [ ] **Step 2: 重写 loadCategories 函数**

找到现有的 `loadCategories(type)` 函数（约第 486-538 行），将其**完全替换**为：

```javascript
        // ===== 分类（两级） =====
        var allCats = []; // 原始分类数据
        var parents = []; // 父分类 [{id, name, icon, children: []}]
        var activeParentId = null; // 当前选中的父分类 ID

        function loadCategories(type) {
            selectedCategoryId = null;
            selectedCategoryName = '';
            activeParentId = null;

            var tabsContainer = document.getElementById('category-tabs');
            var gridContainer = document.getElementById('category-grid');
            tabsContainer.innerHTML = '';
            gridContainer.innerHTML = '<div class="col-span-4 flex items-center justify-center py-8 text-on-surface-variant"><span class="material-symbols-outlined animate-spin mr-2">progress_activity</span>加载分类中...</div>';

            Auth.fetchApi('/categories?type=' + type)
            .then(function (result) {
                if (result.code !== 0) {
                    gridContainer.innerHTML = '<div class="col-span-4 text-center text-on-surface-variant py-8">加载分类失败</div>';
                    return;
                }

                allCats = result.data;

                // 按 parent_id 分组
                var parentMap = {};
                var childrenMap = {};
                var parentList = [];

                allCats.forEach(function (cat) {
                    var pid = cat.parent_id || 0;
                    if (pid === 0) {
                        parentMap[cat.id] = { id: cat.id, name: cat.name, icon: cat.icon || 'more_horiz', children: [] };
                        parentList.push(cat.id);
                    } else {
                        if (!childrenMap[pid]) childrenMap[pid] = [];
                        childrenMap[pid].push(cat);
                    }
                });

                // 将子分类挂载到父分类
                parentList.forEach(function (pid) {
                    parentMap[pid].children = childrenMap[pid] || [];
                });

                parents = parentList.map(function (pid) { return parentMap[pid]; });

                // 渲染 Tab
                renderTabs();

                // 默认选中第一个父分类
                if (parents.length > 0) {
                    selectParent(parents[0].id);
                }
            });
        }

        function renderTabs() {
            var tabsContainer = document.getElementById('category-tabs');
            var html = '';
            parents.forEach(function (p) {
                html += '<div class="parent-tab' + (activeParentId === p.id ? ' active' : '') + '" data-parent-id="' + p.id + '" onclick="window.selectParentTab(' + p.id + ')">';
                html += '<span class="tab-icon material-symbols-outlined" style="font-size:16px">' + p.icon + '</span>';
                html += Auth.escapeHtml(p.name);
                html += '</div>';
            });
            tabsContainer.innerHTML = html;
        }

        window.selectParentTab = function(parentId) {
            activeParentId = parentId;
            renderTabs();
            renderChildren(parentId);
        };

        function selectParent(parentId) {
            activeParentId = parentId;
            renderTabs();
            renderChildren(parentId);
        }

        function renderChildren(parentId) {
            var parent = parents.find(function (p) { return p.id === parentId; });
            if (!parent) return;

            var gridContainer = document.getElementById('category-grid');
            var children = parent.children || [];

            if (children.length === 0) {
                // 父分类无子分类 → 直接显示父分类为可选
                var html = '';
                html += '<div class="col-span-4 empty-sub-msg">该分类暂无子分类</div>';
                html += '<div class="col-span-4 flex flex-col items-center gap-2">';
                html += '<div class="child-btn selected" data-id="' + parent.id + '" data-name="' + Auth.escapeHtml(parent.name) + '" data-icon="' + parent.icon + '" onclick="window.selectCategory(this)">';
                html += '<div class="child-icon" style="background:' + getCategoryBgColor(parent.icon) + '">';
                html += '<span class="material-symbols-outlined" style="font-variation-settings: \'FILL\' 1; font-size:22px;">' + parent.icon + '</span>';
                html += '</div>';
                html += '<div class="child-label" style="color:#004ac6;font-weight:600">' + Auth.escapeHtml(parent.name) + '</div>';
                html += '</div></div>';
                gridContainer.innerHTML = html;

                // 自动选中父分类本身
                selectedCategoryId = parent.id;
                selectedCategoryName = parent.name;
            } else {
                // 有子分类 → 显示子分类网格
                var html = '';
                children.forEach(function (child) {
                    html += '<div class="child-btn" data-id="' + child.id + '" data-name="' + Auth.escapeHtml(child.name) + '" data-icon="' + (child.icon || 'more_horiz') + '" onclick="window.selectCategory(this)">';
                    html += '<div class="child-icon" style="background:' + getCategoryBgColor(child.icon) + '">';
                    html += '<span class="material-symbols-outlined" style="font-variation-settings: \'FILL\' 1; font-size:22px;">' + (child.icon || 'more_horiz') + '</span>';
                    html += '</div>';
                    html += '<div class="child-label">' + Auth.escapeHtml(child.name) + '</div>';
                    html += '</div>';
                });
                gridContainer.innerHTML = html;
            }
        }

        window.selectCategory = function (el) {
            // 取消之前选中
            document.querySelectorAll('.child-btn').forEach(function (b) {
                b.classList.remove('selected');
            });
            // 设置当前选中
            el.classList.add('selected');
            selectedCategoryId = parseInt(el.getAttribute('data-id'));
            selectedCategoryName = el.getAttribute('data-name');

            // 其他支出：显示备注提示
            var tip = document.getElementById('other-remark-tip');
            if (selectedCategoryName === '其他支出') {
                tip.classList.remove('hidden');
            } else {
                tip.classList.add('hidden');
            }
        };

        function getCategoryBgColor(icon) {
            var colors = {
                restaurant: '#fef3c7',
                directions_car: '#e0f2fe',
                shopping_bag: '#ede9fe',
                movie: '#fce7f3',
                home: '#e0e7ff',
                medical_services: '#dcfce7',
                school: '#ccfbf1',
                group: '#fce7f3',
                payments: '#dcfce7',
                more_horiz: '#f1f5f9',
                default: '#dbeafe'
            };
            return colors[icon] || colors.default;
        }
```

- [ ] **Step 3: 修复编辑模式下的分类选中**

找到编辑模式中的分类选中逻辑（约第 699-705 行），将：

```javascript
                setTimeout(function () {
                    document.querySelectorAll('.cat-btn').forEach(function (btn) {
                        if (parseInt(btn.getAttribute('data-id')) === bill.category_id) {
                            btn.click();
                        }
                    });
                }, 500);
```

替换为：

```javascript
                setTimeout(function () {
                    var targetId = bill.category_id;
                    // 查找目标分类属于哪个父分类
                    for (var i = 0; i < parents.length; i++) {
                        var p = parents[i];
                        if (p.id === targetId) {
                            selectParent(p.id);
                            setTimeout(function () {
                                var btn = document.querySelector('.child-btn[data-id="' + targetId + '"]');
                                if (btn) btn.click();
                            }, 50);
                            return;
                        }
                        for (var j = 0; j < p.children.length; j++) {
                            if (p.children[j].id === targetId) {
                                selectParent(p.id);
                                setTimeout(function () {
                                    var btn = document.querySelector('.child-btn[data-id="' + targetId + '"]');
                                    if (btn) btn.click();
                                }, 50);
                                return;
                            }
                        }
                    }
                }, 500);
```

- [ ] **Step 4: 提交**

```bash
git add "003.前端代码/finance/pages/record.html"
git commit -m "feat(sub-category): 记账页实现两级分类 — Tab + 子分类网格"
```

---

### Task 2: category-manage.html — 支持创建子分类

**Files:**
- Modify: `003.前端代码/finance/pages/category-manage.html`

- [ ] **Step 1: 添加"上级分类"选择器到弹窗**

找到弹窗中"类型"字段之后的区域（约第 150-156 行），在类型选择器 `</div>` 之后、按钮区域之前，添加：

```html
                <div>
                    <label class="text-body-sm text-on-surface-variant mb-1 block">上级分类</label>
                    <select id="dialog-parent" class="w-full h-11 bg-surface-container-low border border-outline-variant rounded-lg px-3 text-body-md">
                        <option value="0">无（作为一级分类）</option>
                    </select>
                </div>
```

- [ ] **Step 2: 修改添加分类按钮逻辑 — 填充上级分类选项**

找到 `document.getElementById('btn-add-category').addEventListener(...)` 回调（约第 264-292 行），在渲染图标选择器**之前**，添加加载父分类列表的逻辑：

```javascript
        document.getElementById('btn-add-category').addEventListener('click', function () {
            // 根据当前 tab 预填类型
            dialogType = currentType.toUpperCase() === 'EXPENSE' ? 'EXPENSE' : 'INCOME';
            document.getElementById('dialog-type-expense').className = dialogType === 'EXPENSE' ? 'flex-1 py-2 text-label-caps font-label-caps bg-surface-container-lowest text-danger-expense shadow-sm rounded-lg' : 'flex-1 py-2 text-label-caps font-label-caps text-on-surface-variant rounded-lg';
            document.getElementById('dialog-type-income').className = dialogType === 'INCOME' ? 'flex-1 py-2 text-label-caps font-label-caps bg-surface-container-lowest text-success-growth shadow-sm rounded-lg' : 'flex-1 py-2 text-label-caps font-label-caps text-on-surface-variant rounded-lg';

            // 加载父分类选项
            loadParentCategories();

            // 渲染图标选择器
            ...
```

在 IIFE 中添加 `loadParentCategories` 函数（在 `loadCategories` 函数之后）：

```javascript
        // 加载父分类列表（用于创建子分类时的上级分类选择）
        function loadParentCategories() {
            var select = document.getElementById('dialog-parent');
            select.value = '0'; // 默认无上级

            Auth.fetchApi('/categories?type=' + currentType.toUpperCase())
            .then(function (result) {
                if (result.code !== 0) return;

                var cats = result.data;
                var html = '<option value="0">无（作为一级分类）</option>';
                cats.forEach(function (cat) {
                    if (!cat.parent_id || cat.parent_id === 0) {
                        html += '<option value="' + cat.id + '">' + Auth.escapeHtml(cat.name) + '</option>';
                    }
                });
                select.innerHTML = html;
            });
        }
```

- [ ] **Step 3: 修改 saveNewCategory — 传入 parent_id**

找到 `window.saveNewCategory` 函数（约第 294-313 行），将 body 改为包含 `parent_id`：

```javascript
        window.saveNewCategory = function () {
            var name = document.getElementById('dialog-name').value.trim();
            if (!name) {
                alert('请输入分类名称');
                return;
            }

            var parentId = parseInt(document.getElementById('dialog-parent').value);

            Auth.fetchApi('/categories', {
                method: 'POST',
                body: JSON.stringify({ name: name, icon: selectedIcon, type: dialogType, parent_id: parentId })
            })
            .then(function (result) {
                if (result.code === 0) {
                    closeDialog();
                    loadCategories();
                } else {
                    alert(result.msg || '添加失败');
                }
            });
        };
```

- [ ] **Step 4: 修改分类展示 — 按层级缩进显示**

找到 `loadCategories` 函数中的分类渲染部分（约第 216-236 行），将渲染逻辑替换为支持层级显示的版本：

```javascript
                var cats = result.data;
                var html = '';

                if (cats.length === 0) {
                    html = '<div class="col-span-full text-center text-on-surface-variant py-12">暂无分类</div>';
                } else {
                    // 分组：父分类和子分类
                    var parentList = [];
                    var childrenMap = {};
                    cats.forEach(function (cat) {
                        var pid = cat.parent_id || 0;
                        if (pid === 0) {
                            parentList.push(cat);
                        } else {
                            if (!childrenMap[pid]) childrenMap[pid] = [];
                            childrenMap[pid].push(cat);
                        }
                    });

                    // 渲染：父分类 + 子分类（缩进）
                    parentList.forEach(function (cat) {
                        var icon = cat.icon || 'more_horiz';
                        var children = childrenMap[cat.id] || [];

                        // 父分类
                        html += '<div class="flex flex-col items-center justify-center aspect-square bg-surface-container-lowest shadow-sm rounded-2xl p-card-padding relative border border-transparent hover:border-primary-container transition-all cursor-pointer" data-id="' + cat.id + '" data-name="' + Auth.escapeHtml(cat.name) + '" data-preset="' + cat.is_preset + '" onclick="showCategoryActions(this)">';
                        html += '<div class="w-12 h-12 flex items-center justify-center rounded-full bg-surface-container-highest text-primary mb-2">';
                        html += '<span class="material-symbols-outlined" style="font-variation-settings: \'FILL\' 1;">' + icon + '</span>';
                        html += '</div>';
                        html += '<span class="text-label-caps font-label-caps text-on-surface">' + Auth.escapeHtml(cat.name) + '</span>';
                        if (cat.is_preset) {
                            html += '<span class="text-[9px] text-outline mt-1">系统预设</span>';
                        } else {
                            html += '<span class="text-[9px] text-primary mt-1">自定义</span>';
                        }
                        html += '</div>';

                        // 子分类（缩进显示在父分类下方）
                        if (children.length > 0) {
                            html += '<div class="col-span-full pl-4 border-l-2 border-primary/20 grid grid-cols-3 gap-4 mb-4"></div>';
                            children.forEach(function (child) {
                                var childIcon = child.icon || 'more_horiz';
                                html += '<div class="flex flex-col items-center justify-center aspect-square bg-surface-container-lowest/50 shadow-sm rounded-xl p-card-padding relative border border-transparent hover:border-primary-container/50 transition-all cursor-pointer ml-2" data-id="' + child.id + '" data-name="' + Auth.escapeHtml(child.name) + '" data-preset="' + child.is_preset + '" onclick="showCategoryActions(this)">';
                                html += '<div class="w-10 h-10 flex items-center justify-center rounded-full bg-surface-container text-primary mb-2">';
                                html += '<span class="material-symbols-outlined" style="font-variation-settings: \'FILL\' 1; font-size:20px;">' + childIcon + '</span>';
                                html += '</div>';
                                html += '<span class="text-[11px] font-label-caps text-on-surface">' + Auth.escapeHtml(child.name) + '</span>';
                                if (!child.is_preset) {
                                    html += '<span class="text-[9px] text-primary mt-1">自定义</span>';
                                }
                                html += '</div>';
                            });
                        }
                    });
                }

                grid.innerHTML = html;
```

- [ ] **Step 5: 提交**

```bash
git add "003.前端代码/finance/pages/category-manage.html"
git commit -m "feat(sub-category): 类别管理页支持创建子分类"
```

---

### Task 3: 验证

- [ ] **Step 1: 打开记账页手动验证**

1. 打开 `003.前端代码/finance/pages/record.html`
2. 确认顶部显示父分类 Tab
3. 点击不同父分类，确认下方显示对应子分类
4. 点击子分类，确认选中态（蓝色边框）
5. 切换到收入 Tab，确认分类切换正常

- [ ] **Step 2: 打开类别管理页手动验证**

1. 打开 `003.前端代码/finance/pages/category-manage.html`
2. 确认父分类正常显示
3. 确认子分类在父分类下方缩进显示
4. 点击"添加自定义分类"，确认弹窗中有"上级分类"选择器
5. 选择上级分类后保存，确认新分类作为子分类显示

- [ ] **Step 3: 提交**

如果所有验证通过：

```bash
cd "d:/Java/workspace/2026/claude_my_product"
git add .
git commit -m "feat(sub-category): V1.5 二级分类功能完整实现"
```

---

## 实施顺序

1. **Task 1** → record.html 两级分类（Tab + 子分类网格）
2. **Task 2** → category-manage.html 支持创建子分类
3. **Task 3** → 手动验证

每个 Task 完成后立即提交。
