# 图2 Nacos服务注册与发现时序图

## 图片依据

### 相关代码文件
- `exphlp/api/clientApi/src/main/java/fjnu/edu/NacoaMain.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `scripts/tasks/build-uploaded-alg.sh`
- `scripts/tasks/build-uploaded-alg.ps1`

### 相关配置
- `docker/docker-compose.yml`（Nacos: 8848/9848/9849）

## 图表说明

本图描述真实实现中的 Nacos 时序：算法服务启动后注册实例并发送心跳；平台执行前通过 `DiscoveryClient` 查询实例；执行阶段按服务名调用算法接口。  
当 `DiscoveryClient` 无可用实例时，执行向导会回查 Nacos OpenAPI 以区分“未注册”与“实例不健康”。

## PlantUML代码

```plantuml
@startuml
!theme plain
title Nacos服务注册与发现时序图

participant "算法服务容器" as Alg
participant "Nacos" as Nacos
participant "ExePlanMgrCtrl" as Ctrl
participant "PlanExecuteImpl" as Exec

Alg -> Nacos : 注册实例(serviceName, ip, port)
Nacos --> Alg : 200 OK
loop 每5秒
  Alg -> Nacos : 心跳(beat)
  Nacos --> Alg : ACK
end

Ctrl -> Nacos : DiscoveryClient.getInstances(serviceName)
Nacos --> Ctrl : instances[]
alt 无可用实例
  Ctrl -> Nacos : GET /nacos/v1/ns/instance/list
  Nacos --> Ctrl : total/healthy
end

Exec -> Nacos : DiscoveryClient.getInstances(serviceName)
Nacos --> Exec : instances[]
Exec -> Alg : POST /myAlg/
Alg --> Exec : EachResult[]

@enduml
```

## Mermaid代码

```mermaid
sequenceDiagram
  participant Alg as 算法服务容器
  participant Nacos as Nacos
  participant Ctrl as ExePlanMgrCtrl
  participant Exec as PlanExecuteImpl

  Alg->>Nacos: 注册实例(serviceName/ip/port)
  Nacos-->>Alg: 200 OK
  loop 心跳
    Alg->>Nacos: beat
    Nacos-->>Alg: ACK
  end

  Ctrl->>Nacos: getInstances(serviceName)
  Nacos-->>Ctrl: instances[]
  alt 无可用实例
    Ctrl->>Nacos: /ns/instance/list
    Nacos-->>Ctrl: total/healthy
  end

  Exec->>Nacos: getInstances(serviceName)
  Nacos-->>Exec: instances[]
  Exec->>Alg: POST /myAlg/
  Alg-->>Exec: EachResult[]
```

