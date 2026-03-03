# 图10 算法适配层类图

## 图片依据

### 相关代码文件
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/AlgLibMgrCtrl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/algruntime/service/AlgBuildTaskService.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/algruntime/service/impl/AlgBuildTaskServiceImpl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/NacoaMain.java`

## 图表说明

当前项目中的“算法适配层”不是策略工厂模式，而是三段式适配：  
1. 构建适配：`AlgBuildTaskServiceImpl` 按 `runtimeType` 生成构建脚本与容器。  
2. 调用适配：`PlanExecuteImpl` + `@LoadBalanced RestTemplate` 按服务名调用 `/myAlg/`。  
3. 诊断适配：`ExePlanMgrCtrl.wizardPrecheck` 对实例可达性与 Python 健康接口进行检查。

## PlantUML代码

```plantuml
@startuml
!theme plain
title 算法适配层类图（与当前代码一致）

interface AlgBuildTaskService {
  +createUploadTask(algInfo, file, traceId)
  +triggerBuild(taskId, traceId)
  +getTask(taskId)
  +getLatestTaskByAlgId(algId)
  +tailLog(taskId, tail)
}

class AlgBuildTaskServiceImpl {
  -mongoTemplate
  -objectMapper
  +createUploadTask(...)
  +triggerBuild(...)
  +runBuildTask(...)
  +buildScriptCommand(...)
}

class AlgLibMgrCtrl {
  +uploadSource()
  +buildAndStart()
  +buildStatus()
  +buildLogs()
  +sourceRuntimeInfo()
  +sourceRuntimeOperate()
}

class ExePlanMgrCtrl {
  +preCheck()
  +wizardPrecheck()
}

class PlanExecuteImpl {
  +execute(planId)
  +preCheck(planId)
  +invokeAlgWithRetry(...)
}

class NacoaMain {
  +algRestTemplate() <<@LoadBalanced>>
}

AlgBuildTaskService <|.. AlgBuildTaskServiceImpl
AlgLibMgrCtrl --> AlgBuildTaskService
ExePlanMgrCtrl --> PlanExecuteImpl
PlanExecuteImpl ..> NacoaMain : 使用 algRestTemplate

@enduml
```

## Mermaid代码

```mermaid
classDiagram
  class AlgBuildTaskService {
    <<interface>>
  }
  class AlgBuildTaskServiceImpl
  class AlgLibMgrCtrl
  class ExePlanMgrCtrl
  class PlanExecuteImpl
  class NacoaMain

  AlgBuildTaskService <|.. AlgBuildTaskServiceImpl
  AlgLibMgrCtrl --> AlgBuildTaskService
  ExePlanMgrCtrl --> PlanExecuteImpl
  PlanExecuteImpl ..> NacoaMain
```

