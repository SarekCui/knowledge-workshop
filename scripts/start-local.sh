#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
runtime_dir="/private/tmp/knowledge-workshop"
java_home_value="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home}"
ports=(8083 8081 8082 8084 8087 8080 5173)
services=(iam marketing points learning agent gateway web)

cleanup() {
  trap - EXIT INT TERM
  for service in "${services[@]}"; do
    pid_file="$runtime_dir/$service.pid"
    [[ -f "$pid_file" ]] || continue
    pid="$(<"$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  done
}

trap cleanup EXIT INT TERM

for port in "${ports[@]}"; do
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "启动失败: 端口 $port 已被占用；请先执行 scripts/stop-local.sh。" >&2
    exit 1
  fi
done

for jar in knowledge-iam knowledge-marketing knowledge-points knowledge-learning knowledge-agent knowledge-gateway; do
  if [[ ! -f "$project_dir/$jar/target/$jar-0.1.0-SNAPSHOT.jar" ]]; then
    echo "启动失败: 缺少 $jar 构建产物，请先执行 mvn package -DskipTests。" >&2
    exit 1
  fi
done

mkdir -p "$runtime_dir"
jwt_secret="$(openssl rand -hex 32)"

start_java() {
  local service="$1"
  local module="knowledge-$service"
  local profile=()
  if [[ "$service" == "agent" ]]; then
    profile=(SPRING_PROFILES_ACTIVE=local)
  fi
  env \
    JAVA_HOME="$java_home_value" \
    PATH="$java_home_value/bin:$PATH" \
    JWT_SECRET="$jwt_secret" \
    WEB_AUTH_COOKIE_SECURE=false \
    ${profile[@]+"${profile[@]}"} \
    "$java_home_value/bin/java" -jar "$project_dir/$module/target/$module-0.1.0-SNAPSHOT.jar" \
    >"$runtime_dir/$service.log" 2>&1 &
  echo "$!" >"$runtime_dir/$service.pid"
}

start_java iam
start_java marketing
start_java points
start_java learning
start_java agent
start_java gateway

(
  cd "$project_dir/knowledge-web"
  exec ./node_modules/.bin/vite --host 127.0.0.1 --force
) >"$runtime_dir/web.log" 2>&1 &
echo "$!" >"$runtime_dir/web.pid"

for attempt in {1..60}; do
  ready=true
  for port in "${ports[@]}"; do
    if ! lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      ready=false
      break
    fi
  done
  if [[ "$ready" == true ]]; then
    echo "知识工坊已启动: http://127.0.0.1:5173/"
    echo "运行日志: ${runtime_dir}"
    echo "请保持当前终端运行，按 Ctrl+C 停止全部服务。"
    while true; do
      for service in "${services[@]}"; do
        pid="$(<"$runtime_dir/$service.pid")"
        if ! kill -0 "$pid" 2>/dev/null; then
          echo "服务异常退出: ${service}，请检查 ${runtime_dir}/${service}.log。" >&2
          exit 1
        fi
      done
      sleep 2
    done
  fi
  sleep 1
done

echo "启动超时，未就绪服务的日志位于 ${runtime_dir}。" >&2
exit 1
