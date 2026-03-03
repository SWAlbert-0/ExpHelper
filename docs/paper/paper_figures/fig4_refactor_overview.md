# 图4 重构优化工作概览图

## 图片依据

### 相关代码文件
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/algruntime/service/impl/AlgBuildTaskServiceImpl.java`
- `scripts/ops.ps1`
- `scripts/tasks/check-runtime-readiness.ps1`

### 相关文档
- `docs/dev/开发日志.md`
- `docs/dev/维护手册.md`
- `AGENTS.md`

## 图表说明

本图聚焦“问题 -> 措施 -> 收益”三段链路。  
重构前问题主要是：执行前不可诊断、上传构建链路不闭环、删除语义不一致、文档分散。  
优化措施包括：执行前体检、源码上传构建任务化、删除语义标准化、文档治理收口。  
结果是：执行链路稳定性提升，故障定位前移，平台可维护性增强。

## PlantUML代码

```plantuml
@startuml
!theme plain
title 重构优化工作概览图

rectangle "重构前问题" as P {
  :执行前无服务诊断;
  :源码上传构建缺少闭环状态;
  :删除提示与数据状态不一致;
  :开发文档口径分散;
}
rectangle "优化措施" as M {
  :preCheck/wizardPrecheck;
  :AlgBuildTask + build scripts;
  :deletedCount/noop/verified 统一语义;
  :docs/dev 与 docs/user 长期维护治理;
}
rectangle "结果与收益" as R {
  :执行失败前移诊断;
  :上传->构建->注册->执行闭环;
  :前后端一致性提升;
  :交付与论文证据可追溯;
}

P --> M --> R
@enduml
```

## Mermaid代码

```mermaid
flowchart LR
  P[重构前问题] --> M[优化措施]
  M --> R[结果与收益]
```

