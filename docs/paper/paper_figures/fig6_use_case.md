# 图6 系统用例图

## 图片依据

### 相关代码文件
- `exphlp-front/src/views/problemModel/index.vue`
- `exphlp-front/src/views/algorithm/algorithmConfig.vue`
- `exphlp-front/src/views/planManage/index.vue`
- `exphlp-front/src/views/platform/platformManage.vue`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java`
- `exphlp/api/webApp/src/main/java/fjnu/edu/controller/PlatMgrCtrl.java`

### 相关文档
- `docs/user/快速上手-从部署到执行.md`
- `docs/dev/维护手册.md`

## 图表说明

本图仅包含当前项目真实存在的两类用户：`研究者`、`管理员`。  
研究者可完成问题实例、算法库、执行计划、执行日志与结果查看、个人信息和通知配置等实验流程。  
管理员具备平台账号管理与全局运维权限（新增/编辑/删除用户、重置密码等）。

## PlantUML代码

```plantuml
@startuml
!theme plain
left to right direction
title 系统用例图（两类用户）

actor "研究者" as Researcher
actor "管理员" as Admin

rectangle "算法实验平台" {
  usecase "登录与个人信息维护" as UC1
  usecase "问题实例管理" as UC2
  usecase "算法库管理" as UC3
  usecase "源码上传与构建" as UC4
  usecase "执行前检查" as UC5
  usecase "执行计划管理" as UC6
  usecase "执行日志与结果查看" as UC7
  usecase "通知记录查询/补发" as UC8
  usecase "平台管理(用户管理)" as UC9
}

Researcher --> UC1
Researcher --> UC2
Researcher --> UC3
Researcher --> UC4
Researcher --> UC5
Researcher --> UC6
Researcher --> UC7
Researcher --> UC8

Admin --> UC1
Admin --> UC2
Admin --> UC3
Admin --> UC4
Admin --> UC5
Admin --> UC6
Admin --> UC7
Admin --> UC8
Admin --> UC9
@enduml
```

## Mermaid代码

```mermaid
flowchart LR
  R[研究者]
  A[管理员]

  subgraph S[算法实验平台]
    UC1[登录与个人信息维护]
    UC2[问题实例管理]
    UC3[算法库管理]
    UC4[源码上传与构建]
    UC5[执行前检查]
    UC6[执行计划管理]
    UC7[执行日志与结果查看]
    UC8[通知记录查询/补发]
    UC9[平台管理 用户管理]
  end

  R --> UC1
  R --> UC2
  R --> UC3
  R --> UC4
  R --> UC5
  R --> UC6
  R --> UC7
  R --> UC8

  A --> UC1
  A --> UC2
  A --> UC3
  A --> UC4
  A --> UC5
  A --> UC6
  A --> UC7
  A --> UC8
  A --> UC9
```

