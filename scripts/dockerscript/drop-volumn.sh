#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."
docker compose down -v
echo "초기화 완료"