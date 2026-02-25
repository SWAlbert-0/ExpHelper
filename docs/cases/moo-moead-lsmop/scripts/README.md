# 脚本说明

本目录脚本可直接使用：

1. 启动并等待注册到 Nacos
powershell -ExecutionPolicy Bypass -File docs/cases/moo-moead-lsmop/scripts/start-alg-with-nacos.ps1

2. 连通性检查（Nacos + 算法 HTTP）
powershell -ExecutionPolicy Bypass -File docs/cases/moo-moead-lsmop/scripts/check-nacos-readiness.ps1 -ServiceName moead-lsmop-ls -AlgHttpBase http://localhost:18083

3. 清理示例数据
powershell -ExecutionPolicy Bypass -File docs/cases/moo-moead-lsmop/scripts/cleanup.ps1

