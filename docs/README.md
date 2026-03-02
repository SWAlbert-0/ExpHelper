# 文档总览

为降低查找成本，文档按用途分为四层：

- `docs/user/`：面向使用者的部署、入库、执行、排障手册。
- `docs/dev/`：面向开发者的长期维护文档（开发日志、维护手册、配置清单、错误码映射）。
- `docs/archive/`：历史材料归档（旧报告、转换产物、原始文档等）。
- `docs/cases/`：可运行案例与示例算法（当前包含 NSGA-II + ZDT1）。
- `docs/templates/`：用户自定义算法服务接入模板（Java/Python）。

## 快速入口

- 用户文档入口：`docs/user/README.md`
- 用户先看：`docs/user/快速上手-从部署到执行.md`
- 算法服务接入：`docs/user/算法服务接入与容器化指南.md`
- 用户排障：`docs/user/常见问题与排障.md`
- 脚本总入口：`scripts/ops.ps1`
- 开发日志：`docs/dev/开发日志.md`
- 维护排障：`docs/dev/维护手册.md`
- 配置总表：`docs/dev/配置清单.md`
- 错误码：`docs/dev/错误码映射.md`
- 历史归档：`docs/archive/`
- 示例案例：`docs/cases/moo-nsga2-zdt1/README.md`
- 接入模板：`docs/templates/README.md`

## 阅读优先级

- 用户文档：`docs/user/README.md` -> `快速上手` -> `算法服务接入` -> `常见问题与排障`
- 开发文档：`维护手册` -> `配置清单` -> `错误码映射` -> `开发日志`

## 归档说明

- `docs/archive/` 为历史只读区，用于追溯，不作为当前执行基准。
- 当前可执行标准文档只在 `docs/user/` 与 `docs/dev/` 的长期维护集合中维护。

## 常用脚本

- 统一入口：`scripts/ops.ps1`
- 入口说明：`scripts/README.md`
- 质量门禁：`scripts/ops.ps1 -Action gate`

