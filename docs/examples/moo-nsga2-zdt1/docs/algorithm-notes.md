# 算法说明：NSGA-II for ZDT1-LS100

## 默认参数

- `nVars=100`
- `populationSize=200`
- `maxGenerations=250`
- `crossoverProbability=0.9`（SBX）
- `mutationProbability=0.01`（可替换为 `1/nVars`）
- `seed=20260223`

## 输出约定

算法服务 `/myAlg/` 返回 `List<EachResult>`，其中包含两类记录：

1. `paretoPoint_<idx>`：`value` 为 `"f1=<x>,f2=<y>"`，`dataType="pair"`
2. 摘要指标：
   - `paretoSize`
   - `bestF1`
   - `bestF2`
   - `runtimeMs`

平台执行结果页会基于 `paretoPoint_*` 按需补算并缓存以下对比指标（metricVersion=v1）：

- `HV`（reference point: `(1.1, 1.1)`）
- `IGD+`（ZDT1 理论前沿离散点）
- `Spread(Δ)`（NSGA-II 常用分布性指标）
- `Runtime(ms)`（直接取 `runtimeMs`）

## 复杂度说明

NSGA-II 主要开销在非支配排序与拥挤度计算，典型复杂度接近 `O(MN^2)`，其中：

- `M`：目标数量（本案例为 2）
- `N`：种群规模（默认 200）

## 可调优方向

- 增大种群/代数以提高前沿覆盖
- 减小代数用于快速冒烟
- 固定 `seed` 提升回归可复现性
