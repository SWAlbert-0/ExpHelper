# 脚本入口说明

为降低维护成本，脚本入口统一为 `scripts/ops.ps1`。

## 推荐命令

- 一键启动基础容器 + 算法服务 + 健康检查  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action bootstrap`

- 仅重建并发布 webapp/front  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action deploy`

- 仅启动算法服务（后台常驻）  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-up -Mode offline`

- 仅停止算法服务  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-down`

- 运行时检查  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action check`

- 算法服务检查  
  `powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-check`

## 兼容脚本

旧脚本（如 `up-alg-services.ps1`、`rebuild-webapp-front.ps1`）保留兼容调用，不建议继续新增依赖。
