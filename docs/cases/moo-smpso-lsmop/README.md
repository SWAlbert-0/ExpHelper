# SMPSO + LSMOP 大规模案例测试包（网页交互版）

本目录用于复用平台流程验证：问题实例 -> 算法 -> 执行计划 -> 执行结果与日志。

## 文档入口

- 操作步骤：docs/网页操作步骤.md
- 字段说明：docs/字段与参数说明.md
- 算法说明：docs/algorithm-notes.md

## 术语约定

- 算法服务注册名（Nacos）：smpso-lsmop-ls
- 网页算法服务名：必须填写 smpso-lsmop-ls
- 建议算法名称也填写 smpso-lsmop-ls，减少排错

## 目录说明

- papers/：算法原论文与引用说明
- problem_instances/：问题实例 JSON 样例
- algorithm_instances/：算法库 JSON 导入样例
- scripts/：一键启动、连通性检查、清理脚本
- algorithm_service/：可运行 Java 17 算法服务

## 快速启动

powershell -ExecutionPolicy Bypass -File docs/cases/moo-smpso-lsmop/scripts/start-alg-with-nacos.ps1

## 引用入口

- 案例原论文：papers/references.md
- 指标论文总库：docs/paper/references/metrics/

