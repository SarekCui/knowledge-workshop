#!/usr/bin/env bash
set -euo pipefail

runtime_dir="/private/tmp/knowledge-workshop"
stopped=0

for service in web gateway learning points marketing iam; do
  pid_file="$runtime_dir/$service.pid"
  [[ -f "$pid_file" ]] || continue
  pid="$(<"$pid_file")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    if [[ "$command_line" == *"knowledge-"* || "$command_line" == *"vite"* || "$command_line" == *"npm run dev"* ]]; then
      kill "$pid"
      stopped=$((stopped + 1))
    else
      echo "跳过 $service: PID $pid 不属于知识工坊。" >&2
    fi
  fi
  rm -f "$pid_file"
done

echo "已停止 $stopped 个知识工坊本地进程。"
