# `/admin/*` 残留清单（v2 基线）

统计时间：2026-02-21  
统计命令：`rg "/admin/" exphlp-front/src`

## 基线结果

- 总命中：约 105 处
- `src/api`：约 102 处（主要残留）
- `src/views`：约 1 处
- `src/components`：约 2 处

## 模块分布（高优先级）

- `exphlp-front/src/api/vadmin/permission/*`
- `exphlp-front/src/api/vadmin/system/*`
- `exphlp-front/src/api/vadmin/monitor/*`
- `exphlp-front/src/api/vadmin/menu.js`

## 入口现状

- 已移除顶部消息入口与 `/user/msg` 路由。
- 已移除 `/dict` 隐藏路由入口。
- 已禁用动态模板路由注入（`permission.js` 固定业务路由模式）。

## 隔离策略（当前生效）

- 前端请求拦截器对 `"/admin/*"` 调用直接拒绝并提示“该功能已下线，请使用实验助手业务模块”。
- 保守策略：先阻断误用入口，再分批物理删除未使用 API 文件。

## 下一步删除顺序

1. 删除 `menu.js` 与 `permission.js` 中未使用导入/逻辑（已部分完成）。
2. 清理 `vadmin/system/*` 里无任何业务页面引用的 API 文件。
3. 清理 `vadmin/permission/*` 非个人中心链路 API。
4. 清理 `vadmin/monitor/*` 与 `vadmin/tool/*` 模板 API。

