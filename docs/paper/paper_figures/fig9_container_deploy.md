# 图9 容器化部署架构图

## 图片依据

### 相关代码文件
- `docker/docker-compose.yml`
- `docker/webapp/Dockerfile`
- `docker/frontend/Dockerfile`
- `docker/alg-runner/Dockerfile`
- `scripts/ops.ps1`
- `scripts/tasks/check-runtime-readiness.ps1`

## 图表说明

本图基于 `docker-compose.yml` 真实服务定义：  
核心容器包括 `c_exphlp_vue_front`、`c_exphlp_webapp`、`c_exphlp_nacos`、`c_exphlp_mongo`、`c_exphlp_rabbitmq`。  
算法容器包括固定示例容器和源码上传后动态构建容器。  
WebApp 通过 Docker Socket 执行构建/启动/下线等运维动作，并通过 Nacos 发现算法服务实例。

## PlantUML代码

```plantuml
@startuml
!theme plain
title 容器化部署架构图（Docker Compose）

node "Docker Host" {
  component "c_exphlp_vue_front\n:8086->80" as Front
  component "c_exphlp_webapp\n:8080->8080" as Web
  component "c_exphlp_nacos\n:8848/9848/9849" as Nacos
  database "c_exphlp_mongo\n:27017" as Mongo
  queue "c_exphlp_rabbitmq\n:5672/15672" as MQ

  component "示例算法容器\nc_nsga2/c_moead/c_smpso/c_spea2" as DemoAlg
  component "动态算法容器\nc_alg_{service}_{id}" as DynAlg
}

Front --> Web
Web --> Mongo
Web --> Nacos
Web --> MQ
Web --> DemoAlg : POST /myAlg/
Web --> DynAlg : POST /myAlg/
DemoAlg --> Nacos : 注册/心跳
DynAlg --> Nacos : 注册/心跳

@enduml
```

## Mermaid代码

```mermaid
flowchart TB
  subgraph Host[Docker Host]
    FE[c_exphlp_vue_front]
    WEB[c_exphlp_webapp]
    NACOS[c_exphlp_nacos]
    MONGO[c_exphlp_mongo]
    RMQ[c_exphlp_rabbitmq]
    DEMO[示例算法容器]
    DYN[动态算法容器]
  end

  FE --> WEB
  WEB --> MONGO
  WEB --> NACOS
  WEB --> RMQ
  WEB --> DEMO
  WEB --> DYN
  DEMO --> NACOS
  DYN --> NACOS
```

