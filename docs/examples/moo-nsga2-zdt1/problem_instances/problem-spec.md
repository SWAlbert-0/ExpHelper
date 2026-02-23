# 问题定义：ZDT1（大规模变量）

## 基本形式

- 决策变量：`x = (x1, x2, ..., xn)`，其中 `n=100`
- 变量范围：`xi in [0, 1]`
- 目标函数：
  - `f1(x) = x1`
  - `g(x) = 1 + 9 * sum(x2..xn) / (n - 1)`
  - `h(x) = 1 - sqrt(f1 / g)`
  - `f2(x) = g * h`

## Pareto 前沿

当 `x2..xn = 0` 时，`g=1`，得到：

- `f2 = 1 - sqrt(f1)`
- `f1 in [0,1]`

## 与平台数据映射

- 问题实例通过 `/api/ProbController/addProblem` 入库。
- 本案例中，问题维度等参数作为“算法运行参数（runParas）”传入：
  - `nVars`
  - `populationSize`
  - `maxGenerations`
  - `crossoverProbability`
  - `mutationProbability`
  - `seed`
