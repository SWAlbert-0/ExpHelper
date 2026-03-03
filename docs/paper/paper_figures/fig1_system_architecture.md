# 图1 系统总体架构图

## 图片依据

### 相关代码文件
- `docker/docker-compose.yml`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp-front/src/views/`

### 相关文档
- `docs/dev/维护手册.md`
- `docs/dev/Nacos功能与开发说明.md`
- `docs/user/快速上手-从部署到执行.md`

## 图表说明

本图展示当前项目的总体模块边界与关键交互。系统由前端、WebApp 控制层、领域编排层、基础设施层和算法服务层构成。  
前端通过 HTTP 调用 WebApp API；WebApp 在执行计划时通过 Nacos 按 `serviceName` 发现算法实例并调用 `/myAlg/`。  
MongoDB 承载计划、日志、结果、用户与通知等业务数据，RabbitMQ 当前作为基础设施保留（非执行主链路）。  
算法服务包含 Java 与 Python 两类实现，统一暴露 `/myAlg/` 协议接口。

## PlantUML代码

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

title 系统总体架构图（与当前实现对齐）

actor "研究者/管理员" as User
component "前端 exphlp_front\n(Vue2 + Element)" as Front
component "WebApp API\n(Auth/Prob/Alg/ExePlan/Notification/Plat)" as Web
component "执行编排\nPlanExecuteImpl" as Exec
database "MongoDB\n(expHlp)" as Mongo
component "Nacos\n(service registry)" as Nacos
queue "RabbitMQ\n(infra reserved)" as MQ
component "算法服务(Java/Python)\nGET/POST /myAlg/" as AlgSvc

User --> Front : 浏览器操作
Front --> Web : HTTP API
Web --> Exec : 计划执行/预检查
Exec --> Nacos : DiscoveryClient 查询实例
Exec --> AlgSvc : POST /myAlg/
Web --> Mongo : 读写业务数据
Web --> MQ : 基础设施连接
AlgSvc --> Nacos : 注册/心跳

@enduml
```

## Mermaid代码

```mermaid
flowchart LR
  User[研究者/管理员] --> Front[前端 exphlp_front]
  Front --> Web[WebApp API Controllers]
  Web --> Exec[PlanExecuteImpl]
  Exec --> Nacos[Nacos 服务注册中心]
  Exec --> Alg[Java/Python 算法服务 /myAlg/]
  Web --> Mongo[(MongoDB)]
  Web --> MQ[(RabbitMQ 基础设施保留)]
  Alg --> Nacos
```

