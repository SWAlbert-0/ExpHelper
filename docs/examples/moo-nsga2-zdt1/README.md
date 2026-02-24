# NSGA-II + ZDT1 大规模案例测试包（网页交互版）

本目录用于通过网页交互验证一条完整的多目标优化执行流程（问题实例 -> 算法 -> 执行计划 -> 结果查看/重执行）。

## 文档入口

- 平台用户快速上手：`docs/user/快速上手-从部署到执行.md`
- 操作步骤文档：`docs/网页操作步骤.md`
- 说明文档（字段含义/填写建议/异常处理）：`docs/字段与参数说明.md`
- 算法说明：`docs/algorithm-notes.md`
- 测试记录模板：`docs/test-report-template.md`

## 术语约定（请统一）

- 算法服务注册名（Nacos）：`nsga2-zdt1-ls`
- 网页算法“服务名”字段：必须填写 `nsga2-zdt1-ls`
- 为减少排错，网页算法“算法名称”建议也填写 `nsga2-zdt1-ls`

## 目录说明

- `papers/`：论文 PDF 与引用信息
- `problem_instances/`：问题实例样例与问题定义
- `algorithm_service/`：可运行 Java 算法服务（服务名 `nsga2-zdt1-ls`）
  - `Dockerfile`：用于通过 docker compose 启动并注册到 Nacos
- `scripts/`：辅助脚本
  - `start-alg-with-nacos.ps1`：一键启动算法服务并等待注册到 Nacos（推荐）
  - `check-nacos-readiness.ps1`：一键检查 Nacos/算法服务可用性
- `docs/`：步骤、说明、记录模板

## 快速启动

先确认 Nacos 端口映射（2.x 必需）：
- `8848`（HTTP 控制台/API）
- `9848`（gRPC，服务注册必需）
- `9849`（集群通信，建议同时映射）

```powershell
powershell -ExecutionPolicy Bypass -File docs/examples/moo-nsga2-zdt1/scripts/start-alg-with-nacos.ps1
```

若脚本返回成功，即表示服务 `nsga2-zdt1-ls` 已注册到 Nacos，可直接在网页继续操作。

也可直接走容器方式（推荐给平台使用者）：

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build nsga2_zdt1_example
```

## 常见问题（本次版本新增）

1. 执行结果弹窗提示“系统接口404异常”
- 含义：当前后端镜像尚未包含 `GET /api/AlgRltSaveController/getExeResultDetail`。
- 现行为：前端会自动降级到旧接口并展示基础结果（无完整指标），同时给出升级提示。
- 建议操作：重建并重启 `webapp` 容器，确保前后端版本一致。

2. 删除计划提示成功但列表未变化
- 新版删除接口会返回结构化状态：`deletedCount/noop/blocked`。
- 页面会按返回值显示：
  - `deletedCount>0`：已实际删除。
  - `noop=true`：记录已不存在，页面同步刷新。
  - `blocked=true`：计划执行中，不允许删除。
