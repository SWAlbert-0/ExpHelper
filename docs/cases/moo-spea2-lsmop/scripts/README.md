# 脚本说明

本目录脚本可直接使用：

1. 启动并等待注册到 Nacos
powershell -ExecutionPolicy Bypass -File docs/cases/moo-spea2-lsmop/scripts/start-alg-with-nacos.ps1

2. 连通性检查（Nacos + 算法 HTTP）
powershell -ExecutionPolicy Bypass -File docs/cases/moo-spea2-lsmop/scripts/check-nacos-readiness.ps1 -ServiceName spea2-lsmop-ls -AlgHttpBase http://localhost:18085

3. 清理示例数据
powershell -ExecutionPolicy Bypass -File docs/cases/moo-spea2-lsmop/scripts/cleanup.ps1

