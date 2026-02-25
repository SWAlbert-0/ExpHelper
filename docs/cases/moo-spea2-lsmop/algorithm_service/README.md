# algorithm_service（SPEA2）

该服务为可运行联调骨架，已对齐平台调用协议：
- POST /myAlg/
- 返回 List<EachResult>，含 paretoPoint_* 与 runtimeMs 等字段

当前默认配置：
- ALG_SERVICE_NAME=spea2-lsmop-ls
- ALG_PORT=18085

快速运行：

mvn spring-boot:run

版本基线（与仓库一致）：
- Java 17
- Spring Boot 2.7.18
- Spring Cloud 2021.0.8
- Spring Cloud Alibaba 2021.0.5.0

注意：当前求解内核暂复用 NSGA-II 逻辑作为占位实现，接口稳定后再切换为 SPEA2 真实实现。
