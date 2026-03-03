# 图7 跨语言服务调用时序图

## 图片依据

### 相关代码文件
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/NacoaMain.java`
- `docs/templates/python-fastapi-nacos/main.py`
- `scripts/tasks/build-uploaded-alg.sh`

## 图表说明

本图展示真实跨语言执行链路：  
前端触发执行 -> WebApp 受理 -> 执行器通过 Nacos 发现实例 -> `POST /myAlg/` 调用 Java/Python 算法服务 -> 返回 `EachResult[]` -> 平台持久化。  
协议层统一为 `HTTP + JSON`，并通过统一端点实现语言无关调用。

## PlantUML代码

```plantuml
@startuml
!theme plain
title 跨语言服务调用时序图

actor "研究者" as User
participant "前端" as Front
participant "ExePlanMgrCtrl" as Ctrl
participant "PlanExecuteImpl" as Exec
participant "Nacos" as Nacos
participant "Java/Python算法服务" as Alg
database "MongoDB" as DB

User -> Front : 点击执行
Front -> Ctrl : POST /api/ExePlanController/execute
Ctrl -> Exec : execute(planId)
Exec -> Nacos : getInstances(serviceName)
Nacos --> Exec : instances[]
Exec -> Alg : POST /myAlg/ (AlgRunCtx JSON)
Alg --> Exec : EachResult[]
Exec -> DB : 保存结果/日志
Exec --> Ctrl : accepted/state
Ctrl --> Front : API响应
Front --> User : 展示状态与结果
@enduml
```

## Mermaid代码

```mermaid
sequenceDiagram
  actor U as 研究者
  participant F as 前端
  participant C as ExePlanMgrCtrl
  participant E as PlanExecuteImpl
  participant N as Nacos
  participant A as Java/Python算法服务
  participant M as MongoDB

  U->>F: 执行计划
  F->>C: POST /api/ExePlanController/execute
  C->>E: execute(planId)
  E->>N: getInstances(serviceName)
  N-->>E: instances
  E->>A: POST /myAlg/ (JSON)
  A-->>E: EachResult[]
  E->>M: 写入结果与日志
  E-->>C: accepted/state
  C-->>F: response
  F-->>U: 可视化展示
```

