from fastapi import FastAPI
from typing import Any, Dict

app = FastAPI(title="python-fastapi-template")


@app.get("/myAlg/")
def health() -> Dict[str, Any]:
    return {"status": "ok", "service": "python-template"}


@app.post("/myAlg/")
def run_alg(payload: Dict[str, Any] | None = None) -> Dict[str, Any]:
    return {
        "status": "SUCCESS",
        "reasonCode": "OK",
        "message": "模板算法执行成功，请替换为你的真实算法实现",
        "inputEcho": payload or {},
    }
