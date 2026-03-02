# Python NSGA-II + ZDT1 案例（上传构建链路）

本案例用于验证 Python 算法服务在平台中的完整链路：

问题实例入库 -> 算法入库(runtimeType=python) -> 上传 zip 源码 -> 构建并启动 -> Nacos 注册 -> 执行计划 -> 查看指标。

## 目录

- `algorithm_instances/`：算法导入 JSON
- `problem_instances/`：问题实例导入 JSON
- `algorithm_service/`：可直接打包为 zip 的 Python FastAPI 算法服务
- `docs/网页操作步骤.md`：按页面点击路径执行
- `docs/字段与参数说明.md`：每个填写项的含义和推荐值
- `papers/references.md`：论文与本地参考文献映射
- `scripts/`：本案例的本地检查脚本

## 关键约束

- 算法服务名：`py-nsga2-zdt1-ls`
- 算法库 `serviceName` 必须与 Nacos 注册名一致
- 算法库 `runtimeType` 必须为 `python`
- 上传 zip 的根目录必须包含 `exphlp-alg.json`

## 快速验证（网页）

1. 在“问题实例管理”导入 `problem_instances/zdt1-py-caseA.json`
2. 在“算法库管理”导入 `algorithm_instances/py-nsga2-zdt1-ls-default.json`
3. 在算法行点击“源码”，上传 `algorithm_service` 打包 zip，执行“上传源码 -> 构建并启动”
4. 在“执行计划管理”使用执行向导创建计划并执行
5. 在“执行结果”确认至少有 `HV / IGD+ / GD / Spread / Spacing` 可见

## 说明

该案例用于上传构建与 Python 调用链验证，结果数值用于联通测试，不作为论文实验最终结论数据。
