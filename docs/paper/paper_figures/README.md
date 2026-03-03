# 论文图表文档汇总（已按当前项目事实校准）

本目录存放论文图表的可复现描述文件，全部基于当前仓库真实代码与配置，避免臆造类和不存在调用链。

## 图表列表

| 序号 | 文件名 | 图表名称 | 建议章节 |
|---|---|---|---|
| 1 | `fig1_system_architecture.md` | 系统总体架构图 | 总体设计 |
| 2 | `fig2_nacos_sequence.md` | Nacos服务注册与发现时序图 | 服务治理 |
| 3 | `fig3_tech_stack.md` | 系统技术架构图 | 技术选型 |
| 4 | `fig4_refactor_overview.md` | 重构优化工作概览图 | 前置重构 |
| 5 | `fig5_subsystem_logic.md` | 算法执行子系统逻辑图 | 执行流程设计 |
| 6 | `fig6_use_case.md` | 系统用例图（两类用户） | 需求分析 |
| 7 | `fig7_cross_language_sequence.md` | 跨语言服务调用时序图 | 跨语言协同 |
| 8 | `fig8_data_model.md` | 数据交互模型类图 | 数据模型 |
| 9 | `fig9_container_deploy.md` | 容器化部署架构图 | 部署设计 |
| 10 | `fig10_algorithm_adapter.md` | 算法适配层类图 | 详细设计 |

## 本次校准重点

- 用例参与者统一为两类：`研究者`、`管理员`。
- 路径命名统一为当前项目真实包路径：`fjnu/edu/...`。
- 删除未落地的抽象设计（如 `AlgorithmFactory` 等虚构核心类）。
- 跨语言调用统一按真实协议表达：`HTTP + JSON`，接口为 `/myAlg/`。

## 使用方式

1. 在对应 `fig*.md` 中复制 PlantUML 或 Mermaid 代码生成图片。  
2. 若论文模板要求统一风格，可在绘图工具中调整字体/线宽，但不改语义。  
3. 每次代码架构变化后，优先更新“图片依据”路径，再更新图内容。
