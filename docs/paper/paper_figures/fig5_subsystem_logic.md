# 图5 算法执行子系统逻辑图

## 图片依据

### 相关代码文件
- `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `exphlp/domain/exePlanMgr/src/main/java/fjnu/edu/exePlanMgr/dao/ExePlanMgrDao.java`
- `exphlp/api/clientApi/src/main/java/fjnu/edu/notify/service/impl/NotificationServiceImpl.java`

## 图表说明

本图描述执行子系统真实主流程：  
1. 控制器受理执行请求。  
2. 执行器读取计划并执行预检查。  
3. 通过后进入算法/问题实例循环调用 `/myAlg/`，失败重试并写日志。  
4. 成功或异常均会收敛执行状态并触发通知入队。  
5. `finally` 阶段写入结束时间并释放运行锁。

## PlantUML代码

```plantuml
@startuml
!theme plain
title 算法执行子系统逻辑图

start
:POST /api/ExePlanController/execute;
:读取计划 + 写 executionId;
:preCheck(planId);
if (检查通过?) then (否)
  :标记异常结束;
  :写 PLAN_FAIL 日志;
  stop
else (是)
  :标记执行中 + PLAN_START;
  repeat
    :构建 AlgRunCtx;
    :POST /myAlg/;
    if (调用失败?) then (是)
      :重试并写 RETRY 日志;
    endif
    :聚合并保存结果;
  repeat while (算法/实例循环未结束?)
  :标记正常结束或异常结束;
  :通知入队(MAIL_NOTIFY);
endif
:finally 更新结束时间/释放运行锁;
stop
@enduml
```

## Mermaid代码

```mermaid
flowchart TD
  A[execute API] --> B[读取计划+写executionId]
  B --> C{preCheck通过?}
  C -- 否 --> D[异常结束 + PLAN_FAIL]
  C -- 是 --> E[执行中 + PLAN_START]
  E --> F[循环调用 /myAlg/]
  F --> G[保存聚合结果]
  G --> H[收敛状态 + 通知入队]
  D --> I[finally]
  H --> I[finally]
```

