# Python FastAPI 模板

## 1. 本地运行

```powershell
cd docs/templates/python-fastapi-nacos
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 18083
```

## 2. 构建镜像

```powershell
docker build -t my-python-alg:latest .
```

## 3. 接入平台

1. 算法库管理新增算法，`runtimeType=python`，`serviceName` 填你的注册名。
2. 将源码打 zip 上传时，根目录保留 `exphlp-alg.json`，且其中 `serviceName` 与算法库一致。
3. 上传 zip 不要包含 `.venv/`、`__pycache__/`、`.idea/` 等目录，避免包体过大。
4. 构建并启动后，确认执行检查通过再执行计划。
