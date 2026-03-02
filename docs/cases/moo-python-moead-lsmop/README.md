# Python MOEA/D + LSMOP 案例（上传构建链路）

本案例用于验证第二个 Python 算法服务接入，重点覆盖：

- 多算法并存时的注册与执行稳定性
- 计划日志与结果页面的跨算法可用性
- Coverage 等跨算法指标的比较前提（同计划含多个算法）

## 目录

- `algorithm_instances/`：算法导入 JSON
- `problem_instances/`：问题实例导入 JSON
- `algorithm_service/`：可直接打包为 zip 的 Python FastAPI 算法服务
- `docs/网页操作步骤.md`：按页面点击路径执行
- `docs/字段与参数说明.md`：每个填写项的含义和推荐值
- `papers/references.md`：论文与本地参考文献映射
- `scripts/`：本案例的本地检查脚本

## 关键约束

- 算法服务名：`py-moead-lsmop-ls`
- 算法库 `serviceName` 必须与 Nacos 注册名一致
- 算法库 `runtimeType` 必须为 `python`
- 上传 zip 的根目录必须包含 `exphlp-alg.json`

## 快速验证（网页）

1. 在“问题实例管理”导入 `problem_instances/lsmop-py-caseB.json`
2. 在“算法库管理”导入 `algorithm_instances/py-moead-lsmop-ls-default.json`
3. 上传 `algorithm_service` zip 并构建启动
4. 执行计划中将本算法与另一个算法放在同一计划对比执行
5. 查看执行日志与执行结果，确认指标与通知链路可用

## 说明

LSMOP 家族问题在当前平台结果计算中主要用于链路验证与对比实验准备，指标完整性依赖问题族识别与参考前沿。
