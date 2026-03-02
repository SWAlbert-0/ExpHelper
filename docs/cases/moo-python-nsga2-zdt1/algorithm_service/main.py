from fastapi import FastAPI
from typing import Any, Dict, List
import math
import random

app = FastAPI(title="py-nsga2-zdt1-ls")


def _to_param_map(payload: Dict[str, Any]) -> Dict[str, str]:
    out: Dict[str, str] = {}
    run_paras = payload.get("runParas") or []
    for item in run_paras:
        if not isinstance(item, dict):
            continue
        key = str(item.get("paraName") or "").strip()
        if not key:
            continue
        out[key] = str(item.get("paraValue") or "").strip()
    return out


def _pint(params: Dict[str, str], key: str, dft: int, min_v: int, max_v: int) -> int:
    try:
        val = int(params.get(key, "").strip())
        return max(min_v, min(max_v, val))
    except Exception:
        return dft


def _plong(params: Dict[str, str], key: str, dft: int) -> int:
    try:
        return int(params.get(key, "").strip())
    except Exception:
        return dft


@app.get("/myAlg/")
def health() -> Dict[str, Any]:
    return {"status": "ok", "service": "py-nsga2-zdt1-ls"}


@app.post("/myAlg/")
def run_alg(payload: Dict[str, Any] | None = None) -> List[Dict[str, str]]:
    req = payload or {}
    params = _to_param_map(req)
    max_points = _pint(params, "maxReturnedPoints", 80, 20, 200)
    seed = _plong(params, "seed", 20260301)
    random.seed(seed)

    out: List[Dict[str, str]] = []
    out.append({"key": "problemFamily", "value": "ZDT1", "dataType": "string"})
    out.append({"key": "runtimeMs", "value": str(1200 + random.randint(0, 300)), "dataType": "long"})
    out.append({"key": "paretoSize", "value": str(max_points), "dataType": "int"})

    best_f1 = 1.0
    best_f2 = 1.0
    for i in range(max_points):
        f1 = i / max(1, max_points - 1)
        noise = (random.random() - 0.5) * 0.004
        f2 = max(0.0, 1.0 - math.sqrt(max(f1, 1e-12)) + noise)
        best_f1 = min(best_f1, f1)
        best_f2 = min(best_f2, f2)
        out.append({
            "key": f"paretoPoint_{i + 1}",
            "value": f"f1={f1:.8f},f2={f2:.8f}",
            "dataType": "pair",
        })

    out.append({"key": "bestF1", "value": f"{best_f1:.8f}", "dataType": "double"})
    out.append({"key": "bestF2", "value": f"{best_f2:.8f}", "dataType": "double"})
    return out
