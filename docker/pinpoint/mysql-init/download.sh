#!/usr/bin/env bash
# pinpoint-web 의 mysql init SQL 다운로더. `PINPOINT_VERSION=3.0.4` 등으로 버전 override.
# 버전 bump 후에는 기존 *.sql 을 먼저 `rm` 한 뒤 다시 실행.
set -euo pipefail

PINPOINT_VERSION="${PINPOINT_VERSION:-3.0.5}"
if [[ ! "${PINPOINT_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "PINPOINT_VERSION 값이 올바르지 않습니다: ${PINPOINT_VERSION} (예: 3.0.5)" >&2
    exit 1
fi

TARGET_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAW_BASE="https://raw.githubusercontent.com/pinpoint-apm/pinpoint"
BASE_URL="${RAW_BASE}/v${PINPOINT_VERSION}/web/src/main/resources/sql"
FILES=(
    "CreateTableStatement-mysql.sql"
    "SpringBatchJobRepositorySchema-mysql.sql"
)

for file in "${FILES[@]}"; do
    target="${TARGET_DIR}/${file}"
    if [ -f "${target}" ]; then
        echo "이미 존재: ${target} — skip"
        continue
    fi
    echo "${BASE_URL}/${file} 다운로드 중"
    curl -fL --retry 3 -o "${target}.tmp" "${BASE_URL}/${file}"
    mv "${target}.tmp" "${target}"
done

echo "완료. mysql init SQL 이 ${TARGET_DIR}/ 에 준비됨"
