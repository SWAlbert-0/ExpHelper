# 脚本入口说明

统一入口：`scripts/ops.ps1`  
统一目标：减少脚本分散、降低重复维护成本。

## 目录分层

- `scripts/ops.ps1`：唯一业务入口（推荐所有人只记这一条）。
- `scripts/tasks/`：按职责拆分的原子任务脚本（deploy/check/alg/bootstrap）。
- `scripts/pipelines/`：交付门禁流水线（串联任务并做失败即停）。

## 推荐命令

- 一键启动基础容器 + 算法服务 + 健康检查  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action bootstrap`

- 一键重建并发布 webapp/front  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action deploy`

- 一键运行交付门禁（配置清单 + 单测/构建/e2e + 运行时检查）  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action gate`
  - 无 Docker 权限场景可跳过运行时检查：  
    `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action gate -SkipRuntime`

- 仅运行时检查  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action check`

- 启停/检查算法容器  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-up -Mode offline`  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-check`  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-down`

- 算法源码上传构建（由后端接口触发任务脚本）
  - 实际任务脚本：`scripts/tasks/build-uploaded-alg.ps1`
  - Linux 容器内任务脚本：`scripts/tasks/build-uploaded-alg.sh`
  - 查询入口：算法库管理“源码”弹窗中的任务状态与日志

说明：
- Linux 任务脚本在检查 Nacos 注册时会强制 `curl --noproxy '*'`，避免容器代理变量导致“已注册但检测超时”。

- 一键打开前端/Nacos/快速上手文档  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action onboarding`

## 配置来源（单一真实来源）

- 容器运行配置以 `docker/.env` 为唯一入口。
- `docker/docker-compose*.yml` 只做变量消费，不再硬编码敏感配置。
- 后端默认值在 `exphlp/api/webApp/src/main/resources/application.yml`，生产环境由 `docker/.env` 覆盖。
