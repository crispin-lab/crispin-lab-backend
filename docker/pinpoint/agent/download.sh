#!/usr/bin/env bash
# Pinpoint Java agent 다운로더. `PINPOINT_VERSION=3.0.4` 등으로 버전 override.
set -euo pipefail

PINPOINT_VERSION="${PINPOINT_VERSION:-3.0.5}"
if [[ ! "${PINPOINT_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "PINPOINT_VERSION 값이 올바르지 않습니다: ${PINPOINT_VERSION} (예: 3.0.5)" >&2
    exit 1
fi

TARGET_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCHIVE_NAME="pinpoint-agent-${PINPOINT_VERSION}.tar.gz"
EXTRACTED="${TARGET_DIR}/pinpoint-agent-${PINPOINT_VERSION}"
ARCHIVE="${TARGET_DIR}/${ARCHIVE_NAME}"
RELEASE_BASE="https://github.com/pinpoint-apm/pinpoint/releases/download"
URL="${RELEASE_BASE}/v${PINPOINT_VERSION}/${ARCHIVE_NAME}"
BOOTSTRAP_JAR="${EXTRACTED}/pinpoint-bootstrap-${PINPOINT_VERSION}.jar"

if [ -d "${EXTRACTED}" ]; then
    echo "Pinpoint agent ${PINPOINT_VERSION} 가 이미 ${EXTRACTED} 에 풀려 있어 skip 합니다."
    exit 0
fi

# 부분 추출본을 정리 — 안 그러면 다음 실행이 "이미 존재" 분기로 빠져 깨진 상태가 굳어진다.
cleanup_partial() {
    rm -f "${ARCHIVE}"
    rm -rf "${EXTRACTED}"
}
trap 'cleanup_partial' ERR

echo "Pinpoint agent ${PINPOINT_VERSION} 를 ${URL} 에서 받는 중"
curl -fL --retry 3 -o "${ARCHIVE}" "${URL}"

echo "${TARGET_DIR} 에 압축 해제"
tar -xzf "${ARCHIVE}" -C "${TARGET_DIR}"
rm "${ARCHIVE}"

if [ ! -f "${BOOTSTRAP_JAR}" ]; then
    echo "오류: bootstrap jar 가 기대한 위치에 없습니다: ${BOOTSTRAP_JAR}" >&2
    echo "      부분 추출본을 제거합니다. 원인을 확인한 뒤 스크립트를 다시 실행해 주세요." >&2
    rm -rf "${EXTRACTED}"
    exit 1
fi

trap - ERR

echo "완료. .env 에 다음을 설정: PINPOINT_AGENT_PATH=${BOOTSTRAP_JAR}"
