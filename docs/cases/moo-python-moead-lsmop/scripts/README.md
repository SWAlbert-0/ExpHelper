# 脚本说明（Python案例）

1. 打包源码为上传 zip
```powershell
powershell -ExecutionPolicy Bypass -File docs/cases/moo-python-moead-lsmop/scripts/package-source-zip.ps1
```

2. 检查 Nacos 注册与算法健康接口
```powershell
powershell -ExecutionPolicy Bypass -File docs/cases/moo-python-moead-lsmop/scripts/check-nacos-readiness.ps1 -ServiceName py-moead-lsmop-ls -AlgHttpBase http://localhost:18087
```
