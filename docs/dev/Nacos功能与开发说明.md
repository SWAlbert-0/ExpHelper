# Nacos 功能与开发说明（长期维护）

> 文档定位：本文件是本项目 Nacos 相关能力的唯一开发说明，面向“从未接触过 Nacos 的开发者”和“首次接手本项目的维护者”。  
> 适用范围：服务注册与发现、执行前检查、算法服务上线/下线、Nacos 健康判断、相关脚本与错误码。  
> 维护要求：凡涉及 Nacos 相关代码、脚本、配置、前端提示改动，必须同步更新本文档。  
> 关联文档：  
> - 运维操作：`docs/dev/维护手册.md`  
> - 配置键基线：`docs/dev/配置清单.md`  
> - 错误码定义：`docs/dev/错误码映射.md`  
> - 迭代轨迹：`docs/dev/开发日志.md`

---

## 1. 给零基础开发者的 Nacos 快速理解

### 1.1 Nacos 在本项目里到底做什么
- Nacos 是“算法服务的通讯录”。
- 算法服务启动后，需要把自己的服务名、IP、端口注册到 Nacos。
- 平台执行计划时，不直接写死某个 IP，而是按服务名去 Nacos 查询可用实例。

可以把它理解为：
- 算法服务：`“我叫 nsga2-zdt1-ls，我现在在 172.x.x.x:18082”`
- Nacos：保存这条信息并维护健康状态
- 平台：执行前先问 Nacos“这个服务现在能不能用”

### 1.2 为什么本项目必须依赖 Nacos
- 算法服务可能被重启、迁移、替换容器，IP/端口会变化。
- 平台需要只依赖“服务名”，由注册中心动态解析实际地址。
- 执行前可做门禁检查，避免运行到一半才出现 `Service Instance cannot be null`。

### 1.3 项目里用到的 Nacos 概念（最小集合）
- `serviceName`：服务名，平台调用的唯一关键字。
- `instance`：某个服务的一个运行实例（host+port）。
- `healthy`：实例健康状态。项目判定“可执行”时以 `healthy > 0` 为准。
- `namespace/group`：逻辑隔离维度。本项目默认 `public` + `DEFAULT_GROUP`。

---

## 2. 系统中的职责边界（避免误解）

### 2.1 Nacos 负责什么
- 服务注册（算法容器上报）
- 服务发现（执行前检查和调用前定位）
- 健康实例统计（可执行判定）

### 2.2 Nacos 不负责什么
- 不保存算法执行结果（由 Mongo + `algRltSave` 负责）
- 不编排执行计划（由 `PlanExecuteImpl` + `exePlanMgr` 负责）
- 不发送通知（由通知模块负责）

---

## 3. 本项目 Nacos 总体架构

### 3.1 组件关系
- 基础容器：`nacos`、`webapp`、`exphlp_front`、`mongodb`、`rabbitmq`
- 算法容器：
  - 示例统一镜像容器（`nsga2/moead/smpso/spea2`）
  - 页面源码上传后动态构建容器（java/python）

### 3.2 核心关系图（文字版）
1. 算法容器启动 -> 向 Nacos 注册 `ALG_SERVICE_NAME`  
2. `webapp/clientApi` 通过 `DiscoveryClient` 查询该服务实例  
3. 执行前检查通过后，执行链路请求 `http://<serviceName>/myAlg/`  
4. 失败时返回 `ALG_SERVICE_NO_INSTANCE/NACOS_UNREACHABLE` 等错误码

### 3.3 部署拓扑与端口说明（必须理解）
- Nacos 容器端口：
  - `8848`：HTTP API 与控制台
  - `9848`：gRPC（Nacos 2.x 服务注册关键端口）
  - `9849`：集群/RAFT 通信端口
- 在 Docker 编排里，`webapp` 与算法容器默认通过容器网络访问 `nacos:8848`。
- 若算法容器不在同一网络，会退回 `host.docker.internal:8848`（见构建脚本逻辑）。
- 结论：仅 `8848` 可访问并不代表注册一定成功；2.x 环境下端口映射与网络连通都要检查。

---

## 4. 关键文件与代码入口（可直接定位）

| 功能 | 文件路径 | 说明 |
| --- | --- | --- |
| 执行前检查主逻辑 | `exphlp/api/clientApi/src/main/java/fjnu/edu/impl/PlanExecuteImpl.java` | `preCheck()` + `preCheckPlanReachability()`，用 `DiscoveryClient.getInstances()` |
| 执行向导检查接口 | `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java` | `/preCheck`、`/wizardPrecheck`，输出可诊断项 |
| Python 健康探测增强 | `exphlp/api/webApp/src/main/java/fjnu/edu/controller/ExePlanMgrCtrl.java` | 对 Python 增加 `GET /myAlg/` 可达性校验 |
| 算法源码运行态接口 | `exphlp/api/webApp/src/main/java/fjnu/edu/controller/AlgLibMgrCtrl.java` | `/sourceRuntimeInfo`、`/sourceRuntimeOperate` |
| 运行时健康检查脚本 | `scripts/tasks/check-runtime-readiness.ps1` | 检查 Nacos 指定服务 healthy 实例数 |
| 算法批量启动脚本 | `scripts/tasks/up-alg-services.ps1` | 启动算法容器并等待 Nacos 注册 |
| 算法状态检查脚本 | `scripts/tasks/check-alg-services.ps1` | 输出容器状态 + Nacos total/healthy |
| 源码上传构建脚本 | `scripts/tasks/build-uploaded-alg.ps1` / `.sh` | 构建并启动用户算法容器、轮询注册结果 |
| 错误码定义 | `exphlp/api/webApp/src/main/java/fjnu/edu/auth/ErrorCode.java` | `ALG_SERVICE_NO_INSTANCE`、`NACOS_UNREACHABLE` 等 |

---

## 5. 配置来源与默认值（新手重点）

### 5.1 主配置来源
- Docker 运行时以 `docker/.env` 为主。
- 编排文件：`docker/docker-compose.yml`。
- 后端默认值：`exphlp/api/webApp/src/main/resources/application.yml`（最终会被环境变量覆盖）。

### 5.2 当前关键配置
- `NACOS_SERVER_ADDR`：默认 `nacos:8848`
- `NACOS_NAMESPACE`：默认 `public`
- `NACOS_GROUP`：默认 `DEFAULT_GROUP`
- `ALG_SERVICE_NAME`：每个算法容器独立设置，必须与算法库 `serviceName` 一致
- Nacos 容器端口：`8848`（HTTP）`9848`（gRPC）`9849`（RAFT）

### 5.3 变更规则
- 配置项变更必须同步 `docs/dev/配置清单.md`。
- 禁止在文档中写入真实密钥。示例值使用占位符。

### 5.4 页面字段与 Nacos 概念映射
| 页面字段 | Nacos 对应概念 | 说明 |
| --- | --- | --- |
| 算法库 `serviceName` | `serviceName` | 平台发现服务的唯一标识，必须与注册名一致 |
| `runtimeType` | 非 Nacos 字段 | 决定健康探测策略（尤其 Python 增加 `/myAlg/` 探测） |
| `ALG_SERVICE_NAME`（容器环境） | `serviceName` | 容器启动后向 Nacos 上报的服务名 |
| Nacos `hosts[].healthy` | 实例健康状态 | 平台执行门禁主要依据 |
| Nacos `hosts[].enabled` | 实例启用状态 | 脚本与后端统计时会结合 `healthy && enabled` |

---

## 6. 端到端数据流程（按用户动作展开）

### 6.1 流程 A：基础环境启动并验证 Nacos
1. 执行：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action bootstrap
```
2. 检查容器：
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```
3. 打开 Nacos 控制台：`http://localhost:8848/nacos`

验收标准：
- `c_exphlp_nacos` 运行中
- 控制台可登录并访问服务列表

### 6.2 流程 B：示例算法注册到 Nacos（容器方式）
1. 启动算法：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-up -Mode offline
```
2. 校验算法状态：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-check
```
3. 脚本会打印每个服务的 `total/healthy`。

验收标准：
- `nsga2-zdt1-ls`、`moead-lsmop-ls`、`smpso-lsmop-ls`、`spea2-lsmop-ls` 均有 `healthy > 0`

### 6.3 流程 C：源码上传构建并注册（页面方式）
1. 页面：算法库管理 -> 选择算法 -> “源码”
2. 上传 ZIP（必须包含 `exphlp-alg.json`）
3. 点击“构建并启动”
4. 观察构建状态流转：`PENDING -> RUNNING -> SUCCESS/FAILED`
5. 后端脚本自动等待 Nacos 注册，成功后显示 healthy 实例

### ZIP 契约要求（必须）
- Java：
  - 含 `pom.xml`
  - 含 `exphlp-alg.json`
  - 不要包含 `target/`、`.idea/`
- Python：
  - 含 `requirements.txt` 或 `pyproject.toml`
  - 含 `exphlp-alg.json`
  - 不要包含 `.venv/`、`__pycache__/`

### `exphlp-alg.json` 最小样例
Java:
```json
{
  "runtimeType": "java",
  "serviceName": "nsga2-zdt1-ls",
  "port": 18082
}
```
Python:
```json
{
  "runtimeType": "python",
  "serviceName": "py-nsga2-zdt1-ls",
  "port": 18086,
  "entry": "main:app"
}
```

### 6.4 流程 D：执行计划前检查与执行
1. 页面触发“执行检查”
2. 后端 `/api/ExePlanController/preCheck` 或 `/wizardPrecheck`
3. 若 `pass=true` 才允许执行
4. 执行过程中记录 `PLAN_START/ALG_CALL/PLAN_DONE/PLAN_FAIL` 日志
5. 结束后列表状态应收敛为“正常结束/异常结束”

### 6.5 Nacos OpenAPI 实例查询示例（定位问题最快）
查询命令：
```powershell
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=nsga2-zdt1-ls&groupName=DEFAULT_GROUP&namespaceId=public"
```

重点字段：
- `hosts[].ip`
- `hosts[].port`
- `hosts[].healthy`
- `hosts[].enabled`

判定逻辑（平台侧实践）：
- 可执行：至少存在一个 `healthy=true` 且 `enabled!=false` 的实例
- 不可执行：
  - `hosts` 为空：通常是未注册或服务名不匹配
  - `hosts` 非空但 `healthy=0`：通常是心跳或健康状态异常

---

## 7. 接口与返回数据语义（前后端协同）

### 7.1 执行计划检查接口
- `GET /api/ExePlanController/preCheck`
- `GET /api/ExePlanController/wizardPrecheck`

失败时关键错误码：
- `ALG_SERVICE_NAME_EMPTY`：服务名为空
- `ALG_SERVICE_NO_INSTANCE`：Nacos 无健康实例
- `NACOS_UNREACHABLE`：Nacos 不可达
- `ALG_ENDPOINT_UNREACHABLE`：Python 健康接口不可达

### 7.2 算法源码运行态接口
- `GET /api/AlgController/sourceRuntimeInfo`
- `POST /api/AlgController/sourceRuntimeOperate`

`sourceRuntimeInfo` 典型字段：
- `containerExists/containerRunning/containerStatus`
- `nacosHealthyCount`
- `canOffline/canOnline/canRestart`
- `taskStatus/taskPhase/taskErrorCode`

### 7.3 执行状态兜底修复机制（关键）
- 列表查询 `getExePlans` 时，如果计划仍是“执行中”，会检查日志终态：
  - 有 `PLAN_DONE` -> 回写“正常结束”
  - 有 `PLAN_FAIL` -> 回写“异常结束”
- 目的：修复历史或异常情况下状态卡住问题。

---

## 8. 开发演进记录（来自 git + 迭代任务）

| 版本/提交 | 主题 | 与 Nacos 相关的实质改动 |
| --- | --- | --- |
| `6d7f57d` | 统一算法容器与脚本治理 | 从多脚本散点改为 `ops.ps1` 统一入口，算法启动与检查流程收敛 |
| `02381fa` | upload-build-nacos-execution E2E 收口 | 完成“上传ZIP -> 构建 -> Nacos注册 -> 执行计划”的端到端链路 |
| 迭代 24~30（见开发日志） | P0/P1 增量治理 | 增强预检查、健康探测、运行态可视化、异常提示与重试路径 |
| 最近状态修复（本地改动） | 执行状态卡住修复 | 新增“执行链路状态专用回写 + 日志终态兜底修复” |

典型历史问题（已纳入当前机制）：
- 容器在运行但 Nacos 无健康实例
- 服务名不一致导致 `Service Instance cannot be null`
- 关闭本地终端后算法进程退出（已改为容器常驻方式）
- 构建后注册超时（脚本增加轮询与日志输出）

### 8.1 典型问题 -> 方案 -> 当前机制（经验沉淀）
| 历史问题 | 直接表现 | 根因 | 当前机制 |
| --- | --- | --- | --- |
| 容器 `Up` 但无法执行 | 执行前检查失败 | 容器存活不等于注册健康 | 以 Nacos healthy 作为唯一可执行判据 |
| 启动算法后仍报 `Service Instance cannot be null` | 执行阶段抛异常 | `serviceName` 配置不一致 | 前置校验 + 向导诊断建议 |
| Python 容器在线但健康实例为 0 | 向导提示无可用实例 | 心跳/代理干扰注册 | 构建脚本禁用代理并增强心跳重试 |
| 执行完成但列表仍显示执行中 | 前端状态不收敛 | 状态回写路径被条件拦截 | 执行专用状态更新 + 日志终态兜底修复 |

---

## 9. 常见故障与标准排查手册

### 9.1 `ALG_SERVICE_NO_INSTANCE`
现象：
- 执行前检查失败，提示服务在 Nacos 无可用实例。

排查：
1. `docker ps` 看算法容器是否存活
2. Nacos 服务列表看 `total/healthy`
3. 核对算法库 `serviceName`
4. 看容器日志是否注册成功

修复：
- 优先执行 `ops.ps1 -Action alg-up -Mode offline`
- 或在源码弹窗执行“构建并启动”

### 9.2 `NACOS_UNREACHABLE`
现象：
- 向导检查提示 Nacos 不可用或不可达。

排查：
1. `curl http://localhost:8848/nacos/`
2. 检查 `docker/.env` 的 `NACOS_SERVER_ADDR`
3. 检查 `webapp` 与算法容器网络配置

### 9.3 容器 Up 但实例不健康
现象：
- Nacos `total > 0` 但 `healthy = 0`

排查：
1. 查看算法服务自身健康接口 `GET /myAlg/`
2. 查看启动参数 `ALG_PORT/ALG_SERVICE_NAME`
3. 若为上传构建的 Python 服务，检查构建日志中的心跳注册信息

### 9.4 执行计划显示“执行中”不收敛
现象：
- 计划实际完成，但页面仍显示执行中。

处理：
1. 刷新计划列表（触发后端兜底修复）
2. 查看计划日志是否存在 `PLAN_DONE/PLAN_FAIL`
3. 若无终态日志，检查执行线程异常与数据库写入路径

### 9.5 首次接手开发者的排障顺序（建议固定执行）
1. `ops.ps1 -Action check`：先看基础容器和 Nacos 实例是否满足门禁。  
2. `ops.ps1 -Action alg-check`：看目标算法服务 `total/healthy`。  
3. 页面执行向导“执行检查”：看结构化错误码与诊断建议。  
4. 若是源码上传路径，进入“源码”弹窗看 `taskStatus/taskPhase/errorCode`。  
5. 对照本文件第 7 节接口语义和第 9 节故障模型做定点修复。

---

## 10. 新开发者 30 分钟上手流程（推荐）

1. 启动基础容器：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action bootstrap
```
2. 启动示例算法：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action alg-up -Mode offline
```
3. 运行体检：
```powershell
powershell -ExecutionPolicy Bypass -File scripts/ops.ps1 -Action check
```
4. 页面执行一次“执行检查 -> 执行计划”
5. 在 Nacos 控制台验证服务健康实例数

完成后你应当能回答：
- 平台是如何通过 `serviceName` 找到算法服务的
- 哪些错误码代表 Nacos 问题
- 哪些脚本可一键定位注册失败

---

## 11. 与论文/答辩相关的可复述要点

1. 本项目将“服务发现”前移为“执行门禁”，显著降低运行时盲调成本。  
2. 平台将 Nacos 健康状态纳入 UI 与脚本双通道校验，形成“可观测 + 可操作”的闭环。  
3. 源码上传构建链路通过契约校验与注册轮询，将算法接入流程标准化。  
4. 针对历史脏状态引入“日志终态回写”，提高执行状态一致性与可维护性。

---

## 12. 文档维护规约（必须执行）

以下改动发生时，必须更新本文件：
- Nacos 地址/命名空间/分组默认值变化
- 执行前检查逻辑变化（包括错误码语义）
- 算法注册链路变化（脚本、接口、容器行为）
- Nacos 健康判定规则变化（healthy/enabled 判据）
- 页面“源码运行态/执行检查”提示语义变化

同步要求：
1. 更新 `docs/dev/Nacos功能与开发说明.md`
2. 若涉及配置，更新 `docs/dev/配置清单.md`
3. 若涉及错误码，更新 `docs/dev/错误码映射.md`
4. 在 `docs/dev/开发日志.md` 追加变更摘要与验证记录
