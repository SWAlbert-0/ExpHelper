# py-moead-lsmop-ls 算法服务源码

本目录可直接打包 zip 上传到“算法库管理 -> 源码”弹窗。

## 最小要求

- 根目录保留 `exphlp-alg.json`
- 包含 `main.py` 与 `requirements.txt`
- `exphlp-alg.json.runtimeType=python`
- `exphlp-alg.json.serviceName=py-moead-lsmop-ls`

## 本地调试（可选）

```powershell
cd docs/cases/moo-python-moead-lsmop/algorithm_service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 18087
```

健康检查：`GET http://localhost:18087/myAlg/`
