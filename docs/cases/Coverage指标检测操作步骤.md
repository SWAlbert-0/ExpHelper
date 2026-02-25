# Coverage 指标检测操作步骤（可执行版）

本文给出一套可复现、可验收的 Coverage 检测流程，用于判断平台中 Coverage 指标是否计算正确、展示正确。

## 1. 先明确系统内 Coverage 定义

当前系统实现（`exphlp/domain/algRltSave/.../AlgRltSaveServiceImpl.java`）为：

- `Coverage(A,B) = 被 A 支配的 B 前沿点数 / B 前沿总点数`
- 仅在同一 `planId + runIndex + probInstId` 下，与“其他算法”做两两比较
- 当前算法若有多个对比算法，Coverage 取均值
- 若没有可对比算法，Coverage 返回 `N/A`（即 `null`），不应判定为失败

因此，检测必须覆盖三类场景：`N/A`、`有效值(0~1)`、`边界值(接近0/接近1)`。

## 2. 检测目标与通过标准

### 2.1 目标

- 验证 Coverage 字段在执行结果页可正确显示
- 验证 Coverage 计算方向正确（A 覆盖 B 与 B 覆盖 A 不应混淆）
- 验证无对比对象时显示 `N/A`，而不是 `0` 或报错

### 2.2 通过标准

1. 单算法计划：Coverage 显示 `N/A`，且状态仍可为正常结束  
2. 双算法计划：Coverage 落在 `[0,1]`，且两算法结果通常不同  
3. 参数拉开后：较优算法 Coverage 显著高于较弱算法（方向正确）  
4. 执行日志、执行结果、邮件通知（如启用）中的 Coverage 一致  

## 3. 准备环境

1. 启动平台
- `powershell -ExecutionPolicy Bypass -File scripts/rebuild-webapp-front.ps1`

2. 启动至少两个可比较算法服务（建议同问题族）
- `moead-lsmop-ls`
- `smpso-lsmop-ls`

可使用：
- `docs/cases/moo-moead-lsmop/scripts/start-alg-with-nacos.ps1`
- `docs/cases/moo-smpso-lsmop/scripts/start-alg-with-nacos.ps1`

3. 在 Nacos 确认两个服务都健康注册。

## 4. 基线实验（验证 N/A 逻辑）

1. 问题实例管理：导入一个 LSMOP 问题实例 JSON  
   例：`docs/cases/moo-moead-lsmop/problem_instances/moead-lsmop-caseA.json`
2. 算法库管理：只导入一个算法 JSON  
   例：`docs/cases/moo-moead-lsmop/algorithm_instances/moead-lsmop-ls-default.json`
3. 创建计划并执行（run=1）。
4. 打开“执行结果”。

预期：
- Coverage 为 `N/A`
- 页面提示“无可对比算法”类说明
- 不应出现异常结束

## 5. 对比实验（验证 Coverage 计算与方向）

1. 在同一计划中添加第二个算法  
   例：导入 `docs/cases/moo-smpso-lsmop/algorithm_instances/smpso-lsmop-ls-default.json`
2. 保持同一问题实例、同一 run 次数（建议 run=3）
3. 执行计划，分别查看两个算法执行结果

预期：
- 两算法 Coverage 都在 `[0,1]`
- 两者通常不相等
- 若两个算法前沿质量有差距，较优者 Coverage 更高

## 6. 灵敏度实验（验证指标随质量变化）

目标：人为拉开算法质量，观察 Coverage 是否按方向变化。

建议做两组计划（同问题实例）：

- 计划 A（较强参数）
  - `populationSize` 较大（如 200）
  - `maxGenerations` 较大（如 300）
- 计划 B（较弱参数）
  - `populationSize` 较小（如 40）
  - `maxGenerations` 较小（如 30）

执行后比较：
- 强参数方案对弱参数方案的 Coverage 应明显偏高
- 若结果反向且稳定复现，需要排查算法输出或 Coverage 计算链路

## 7. 一致性校验（结果/日志/通知）

1. 执行结果弹窗：
- 汇总表 `Coverage`
- run 明细 `Coverage`

2. 执行日志：
- 确认计划正常完成，结果可用

3. 邮件通知（已启用时）：
- 汇总与 run 表中的 Coverage 与页面一致

## 8. 常见误判与排除

1. 把 `N/A` 当成 0  
- 错误。`N/A` 表示“无对比对象”，不是“覆盖率为0”。

1.1 把 `0.000000` 当成系统故障  
- 也错误。`0` 表示“有对比对象，但当前算法没有覆盖到对方前沿点”，是合法计算结果。  
- 在 `moead-lsmop-ls` 与 `smpso-lsmop-ls` 参数接近时，出现 `0` 并不罕见。

2. 不同问题实例之间比较 Coverage  
- 无效。Coverage 是同 runKey（runIndex + probInstId）下比较。

3. 只看单次 run 下结论  
- 不稳。建议 `run>=3`，看均值与趋势。

4. 两算法服务名写错或未注册  
- 会导致执行失败，不属于 Coverage 计算错误。

## 8.1 复现 Coverage > 0 的推荐参数

若你想先验证“系统能算出非0 Coverage”，可在同一计划下使用以下对照参数（同问题实例）：

- 算法A（较强）：
  - `populationSize=300`
  - `maxGenerations=600`
  - `mutationProbability=0.01`
  - `seed=20260225`
- 算法B（较弱）：
  - `populationSize=40`
  - `maxGenerations=30`
  - `mutationProbability=0.2`
  - `seed=20260301`

建议 `runNum=3`，再看 Coverage 均值和 run 明细，通常可观察到至少一侧 Coverage 明显大于 0。

## 9. 验收记录模板（建议）

每次检测至少记录：

- 日期、代码版本（healthz 的 `artifactVersion/buildTime`）
- 问题实例 ID / 算法 ID / 计划 ID
- run 次数
- 两算法 Coverage 均值与每 run 明细
- 结论：通过 / 不通过
- 若不通过：异常截图 + 日志片段 + 复现步骤

---

建议按本流程固定做回归：
- 新增算法接入时
- 指标计算代码变更时
- 前端结果展示改版时
