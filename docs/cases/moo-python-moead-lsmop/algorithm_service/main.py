from fastapi import FastAPI
from typing import Any, Dict, List
import math
import random

app = FastAPI(title="py-moead-lsmop-ls")


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
    return {"status": "ok", "service": "py-moead-lsmop-ls"}


@app.post("/myAlg/")
def run_alg(payload: Dict[str, Any] | None = None) -> List[Dict[str, str]]:
    req = payload or {}
    params = _to_param_map(req)
    max_points = _pint(params, "maxReturnedPoints", 90, 20, 220)
    seed = _plong(params, "seed", 20260302)
    random.seed(seed)

    out: List[Dict[str, str]] = []
    out.append({"key": "problemFamily", "value": "LSMOP", "dataType": "string"})
    out.append({"key": "runtimeMs", "value": str(1700 + random.randint(0, 500)), "dataType": "long"})
    out.append({"key": "paretoSize", "value": str(max_points), "dataType": "int"})

    best_f1 = 1.0
    best_f2 = 1.0
    for i in range(max_points):
        f1 = i / max(1, max_points - 1)
        wave = 0.03 * math.sin(8.0 * math.pi * f1)
        noise = (random.random() - 0.5) * 0.006
        f2 = max(0.0, 1.0 - math.pow(max(f1, 1e-12), 0.65) + wave + noise)
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
