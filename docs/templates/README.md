# 算法服务模板目录

本目录提供可直接接入 ExpHelper 的最小模板：

1. `java-springboot-nacos`：Java 17 + Spring Boot 2.7.18 + Nacos 注册
2. `python-fastapi-nacos`：Python + FastAPI + Uvicorn（示例为直连，Nacos 注册由容器编排侧完成）

两套模板都包含：
- `POST /myAlg/` 执行入口
- `GET /myAlg/` 健康检查入口
- `exphlp-alg.json`（源码上传构建元数据）

使用建议：
- 优先复制模板后改包名/算法逻辑；不要直接在模板目录改业务代码。
- 保持算法库 `serviceName` 与实际注册名一致。
- 上传 zip 时根目录必须包含 `exphlp-alg.json`。
