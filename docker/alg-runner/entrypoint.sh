#!/bin/sh
set -eu

alg_type="${ALG_TYPE:-nsga2}"
jar_path=""

case "$alg_type" in
  nsga2)
    jar_path="/app/runners/nsga2-zdt1-ls.jar"
    ;;
  moead)
    jar_path="/app/runners/moead-lsmop-ls.jar"
    ;;
  smpso)
    jar_path="/app/runners/smpso-lsmop-ls.jar"
    ;;
  spea2)
    jar_path="/app/runners/spea2-lsmop-ls.jar"
    ;;
  *)
    echo "[FAIL] unsupported ALG_TYPE=$alg_type (expected: nsga2/moead/smpso/spea2)"
    exit 1
    ;;
esac

if [ ! -f "$jar_path" ]; then
  echo "[FAIL] jar not found: $jar_path"
  exit 1
fi

echo "[INFO] starting algorithm service: ALG_TYPE=$alg_type, JAR=$jar_path, ALG_SERVICE_NAME=${ALG_SERVICE_NAME:-}, ALG_PORT=${ALG_PORT:-}"
exec java -jar "$jar_path"
