# Java Spring Boot + Nacos 模板

## 1. 本地运行

```powershell
cd docs/templates/java-springboot-nacos
mvn spring-boot:run
```

## 2. 构建镜像

```powershell
docker build -t my-java-alg:latest .
```

## 3. 接入平台

1. 算法库管理新增算法，`serviceName` 填 `java-template-alg`（或你的服务名）。
2. 源码打 zip 上传时，确保根目录有 `exphlp-alg.json`，且其中 `serviceName` 与算法库里填写的服务名一致。
3. 上传 zip 不要包含 `target/`、`.idea/`、`node_modules/` 等构建/IDE 目录，避免体积过大导致上传失败。
4. 构建并启动后，在 Nacos 检查服务实例。
5. 在执行计划管理中使用执行向导运行计划。
