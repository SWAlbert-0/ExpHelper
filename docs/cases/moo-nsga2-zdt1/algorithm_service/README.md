# algorithm_service（NSGA-II）

该服务为可运行联调骨架，已对齐平台调用协议：
- POST /myAlg/
- 返回 List<EachResult>，含 paretoPoint_* 与 runtimeMs 等字段

当前默认配置：
- ALG_SERVICE_NAME=nsga2-zdt1-ls
- ALG_PORT=18082

源码上传注意：
- zip 根目录必须保留 `exphlp-alg.json`
- 不要包含 `target/`、`.idea/` 等目录，避免包体过大导致上传失败

快速运行：

```powershell
mvn spring-boot:run
```

版本基线（与仓库一致）：
- Java 17
- Spring Boot 2.7.18
- Spring Cloud 2021.0.8
- Spring Cloud Alibaba 2021.0.5.0
