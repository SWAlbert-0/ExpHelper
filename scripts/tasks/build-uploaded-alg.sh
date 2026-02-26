#!/usr/bin/env sh
set -eu

TASK_ID=""
ALG_ID=""
SERVICE_NAME=""
RUNTIME_TYPE=""
VERSION=""
SOURCE_ZIP=""
IMAGE_NAME=""
CONTAINER_NAME=""
WORK_ROOT="temp/alg-build"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --task-id) TASK_ID="$2"; shift 2 ;;
    --alg-id) ALG_ID="$2"; shift 2 ;;
    --service-name) SERVICE_NAME="$2"; shift 2 ;;
    --runtime-type) RUNTIME_TYPE="$2"; shift 2 ;;
    --version) VERSION="$2"; shift 2 ;;
    --source-zip) SOURCE_ZIP="$2"; shift 2 ;;
    --image-name) IMAGE_NAME="$2"; shift 2 ;;
    --container-name) CONTAINER_NAME="$2"; shift 2 ;;
    --work-root) WORK_ROOT="$2"; shift 2 ;;
    *) echo "[FAIL] unknown argument: $1"; exit 1 ;;
  esac
done

fail() {
  echo "[FAIL] $1"
  exit 1
}

ensure_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

ensure_cmd docker
ensure_cmd unzip
ensure_cmd curl

[ -n "$TASK_ID" ] || fail "TASK_ID empty"
[ -n "$ALG_ID" ] || fail "ALG_ID empty"
[ -n "$SERVICE_NAME" ] || fail "SERVICE_NAME empty"
[ -n "$RUNTIME_TYPE" ] || fail "RUNTIME_TYPE empty"
[ -n "$SOURCE_ZIP" ] || fail "SOURCE_ZIP empty"
[ -n "$IMAGE_NAME" ] || fail "IMAGE_NAME empty"
[ -n "$CONTAINER_NAME" ] || fail "CONTAINER_NAME empty"
[ -f "$SOURCE_ZIP" ] || fail "source zip not found: $SOURCE_ZIP"

task_dir="$WORK_ROOT/$TASK_ID"
src_dir="$task_dir/src"
rm -rf "$src_dir"
mkdir -p "$src_dir"

echo "[INFO] unzip source: $SOURCE_ZIP -> $src_dir"
unzip -q -o "$SOURCE_ZIP" -d "$src_dir" || true

meta_path="$src_dir/exphlp-alg.json"
if [ ! -f "$meta_path" ]; then
  alt_meta="$(find "$src_dir" -type f -name 'exphlp-alg.json' | head -n 1 || true)"
  [ -n "$alt_meta" ] || fail "missing exphlp-alg.json"
  meta_path="$alt_meta"
fi

meta_runtime="$(jq -r '.runtimeType // ""' "$meta_path" 2>/dev/null || true)"
if [ -n "$meta_runtime" ] && [ "$meta_runtime" != "$RUNTIME_TYPE" ]; then
  fail "runtime mismatch: meta=$meta_runtime, request=$RUNTIME_TYPE"
fi

port="$(jq -r '.port // 18090' "$meta_path" 2>/dev/null || echo 18090)"
entry="$(jq -r '.entry // "main:app"' "$meta_path" 2>/dev/null || echo "main:app")"

network_name="docker_default"
if ! docker network ls --format '{{.Name}}' | grep -q "^$network_name$"; then
  network_name=""
fi

if [ "$RUNTIME_TYPE" = "java" ]; then
  [ -f "$src_dir/pom.xml" ] || fail "java project missing pom.xml"
  cat > "$src_dir/Dockerfile.generated" <<EOF
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . /build
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
ENV SERVER_PORT=$port
ENTRYPOINT ["java","-jar","/app/app.jar"]
EOF
else
  if [ ! -f "$src_dir/requirements.txt" ] && [ ! -f "$src_dir/pyproject.toml" ]; then
    fail "python project missing requirements.txt or pyproject.toml"
  fi
  cat > "$src_dir/start_with_nacos.py" <<EOF
import os
import socket
import threading
import time
import requests
import uvicorn

def local_ip():
    return socket.gethostbyname(socket.gethostname())

def register_nacos():
    nacos = os.getenv("NACOS_SERVER_ADDR", "host.docker.internal:8848")
    if not nacos.startswith("http"):
        nacos = "http://" + nacos
    service_name = os.getenv("ALG_SERVICE_NAME")
    ip = local_ip()
    p = int(os.getenv("ALG_PORT", "$port"))
    namespace_id = os.getenv("NACOS_NAMESPACE", "public")
    group_name = os.getenv("NACOS_GROUP", "DEFAULT_GROUP")
    requests.post(f"{nacos}/nacos/v1/ns/instance", data={
        "serviceName": service_name,
        "ip": ip,
        "port": p,
        "namespaceId": namespace_id,
        "groupName": group_name,
        "healthy": "true",
        "enabled": "true",
        "ephemeral": "true"
    }, timeout=5)
    while True:
        try:
            requests.put(f"{nacos}/nacos/v1/ns/instance/beat", data={
                "serviceName": service_name,
                "ip": ip,
                "port": p,
                "namespaceId": namespace_id,
                "groupName": group_name,
                "beat": '{"ip":"%s","port":%d,"healthy":true}' % (ip, p)
            }, timeout=5)
        except Exception:
            pass
        time.sleep(5)

if __name__ == "__main__":
    threading.Thread(target=register_nacos, daemon=True).start()
    uvicorn.run("$entry", host="0.0.0.0", port=int(os.getenv("ALG_PORT", "$port")))
EOF
  cat > "$src_dir/Dockerfile.generated" <<EOF
FROM python:3.11-slim
WORKDIR /app
COPY . /app
RUN pip install --no-cache-dir -r requirements.txt && pip install --no-cache-dir uvicorn fastapi requests
ENV ALG_PORT=$port
CMD ["python","start_with_nacos.py"]
EOF
fi

echo "[INFO] docker build image: $IMAGE_NAME"
docker build -t "$IMAGE_NAME" -f "$src_dir/Dockerfile.generated" "$src_dir" || fail "docker build failed"

echo "[INFO] remove old container: $CONTAINER_NAME"
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

run_args="run -d --name $CONTAINER_NAME -e ALG_PORT=$port -e ALG_SERVICE_NAME=$SERVICE_NAME -e NACOS_NAMESPACE=public -e NACOS_GROUP=DEFAULT_GROUP"
if [ -n "$network_name" ]; then
  run_args="$run_args -e NACOS_SERVER_ADDR=nacos:8848 --network $network_name"
else
  run_args="$run_args -e NACOS_SERVER_ADDR=host.docker.internal:8848"
fi
if [ "$RUNTIME_TYPE" = "java" ]; then
  run_args="$run_args -e SPRING_APPLICATION_NAME=$SERVICE_NAME"
fi

echo "[INFO] docker $run_args $IMAGE_NAME"
# shellcheck disable=SC2086
docker $run_args "$IMAGE_NAME" >/dev/null || fail "docker run failed"

echo "[INFO] wait nacos healthy: $SERVICE_NAME"
if [ -n "${NACOS_SERVER_ADDR:-}" ]; then
  nacos_addr="$NACOS_SERVER_ADDR"
elif [ -n "$network_name" ]; then
  nacos_addr="nacos:8848"
else
  nacos_addr="host.docker.internal:8848"
fi
case "$nacos_addr" in
  http://*|https://*) nacos_base="$nacos_addr" ;;
  *) nacos_base="http://$nacos_addr" ;;
esac
deadline=$(( $(date +%s) + 90 ))
while [ "$(date +%s)" -lt "$deadline" ]; do
  count_raw="$(curl --noproxy '*' -s "$nacos_base/nacos/v1/ns/instance/list?serviceName=$SERVICE_NAME&groupName=DEFAULT_GROUP&namespaceId=public" | jq -r '[.hosts[]? | select(.healthy==true and .enabled!=false)] | length' 2>/dev/null || echo 0)"
  count="$(echo "$count_raw" | tr -d '\r\n\t ')"
  case "$count" in
    ''|*[!0-9]*) count=0 ;;
  esac
  if [ "$count" -gt 0 ]; then
    echo "[OK] service registered to nacos, healthy=$count"
    exit 0
  fi
  sleep 3
done

fail "wait nacos registration timeout"
