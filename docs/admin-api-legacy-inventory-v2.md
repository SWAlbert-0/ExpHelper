# `/admin/*` 残留清单（v2 基线）

统计时间：2026-02-21  
统计命令：`rg "/admin/" exphlp-front/src`

## 基线结果

- 初始基线（迭代07）：约 105 处
- 当前结果（迭代08）：1 处
- 当前保留项：`exphlp-front/src/utils/request.js` 的拦截哨兵（用于阻断误调用）

## 已完成收敛

- 已删除不可达模板页面：`src/views/vadmin/{monitor,system,tool,permission/*}`（保留个人中心并迁移至 `src/views/profile`）。
- 已删除历史 `/admin/*` API 模块：`src/api/vadmin/{monitor,system,permission,tool}` 相关文件。
- 已移除未使用旧组件与上传入口：`DeptTree`、`UsersTree`、`ModelDisplay`、`FileUpload`。
- 已将认证与个人中心 API 收敛到 `src/api/auth.js`（统一走 `/api/auth/*`）。
- 已完成业务 API 命名迁移：业务页面不再引用 `@/api/vadmin/*`，迁移至 `src/api/exphlp/*`、`src/api/problem.js`、`src/api/algorithm.js`。

## 入口现状

- 已移除顶部消息入口与 `/user/msg` 路由。
- 已移除 `/dict` 隐藏路由入口。
- 已禁用动态模板路由注入（`permission.js` 固定业务路由模式）。
- 个人中心页面入口已迁移为 `src/views/profile`，不再依赖 `views/vadmin`。

## 隔离策略（当前生效）

- 前端请求拦截器对 `"/admin/*"` 调用直接拒绝并提示“该功能已下线，请使用实验助手业务模块”。
- `scripts/check-p0.ps1` 已新增校验：除拦截哨兵外，禁止新增 `/admin/` 引用。

## 下一步

1. 在 CI 中强制执行 `scripts/check-p0.ps1`（含 `/admin/` 与 legacy import baseline 校验）。
2. 对新接口模块补充最小单测或契约检查，防止命名迁移后行为回退。
