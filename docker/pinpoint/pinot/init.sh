#!/bin/sh
# Pinpoint Inspector / metric 용 Pinot 테이블 7종 등록. compose 의 pinot-init 가 한 번 실행.
# 동기와 다운로드 의존성은 dev-infra.md "사전 준비" 참조.

set -e

# image 태그는 `3.0.5`, git tag 는 `v3.0.5` — 같은 값이지만 prefix 가 달라 여기서만 `v` 부착.
VER="${PINPOINT_VERSION:-3.0.5}"
BASE="https://raw.githubusercontent.com/pinpoint-apm/pinpoint/v${VER}"

cd /tmp

curl -sSf "${BASE}/uristat/uristat-common/src/main/pinot/pinot-uriStat-realtime-table.json" -o uriStatTable.json
curl -sSf "${BASE}/uristat/uristat-common/src/main/pinot/pinot-uriStat-schema.json" -o uriStatSchema.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-tag-realtime-table.json" -o tagTable.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-tag-schema.json" -o tagSchema.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-double-realtime-table.json" -o doubleTable.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-double-schema.json" -o doubleSchema.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-dataType-realtime-table.json" -o dataTypeTable.json
curl -sSf "${BASE}/metric-module/metric/src/main/pinot/pinot-dataType-schema.json" -o dataTypeSchema.json
curl -sSf "${BASE}/exceptiontrace/exceptiontrace-common/src/main/pinot/pinot-exceptionTrace-offline-table.json" -o exceptionTraceTable.json
curl -sSf "${BASE}/exceptiontrace/exceptiontrace-common/src/main/pinot/pinot-exceptionTrace-schema.json" -o exceptionTraceSchema.json
curl -sSf "${BASE}/inspector-module/inspector-collector/src/main/pinot/pinot-inspector-stat-agent-realtime-table.json" -o inspectorAgentTable.json
curl -sSf "${BASE}/inspector-module/inspector-collector/src/main/pinot/pinot-inspector-stat-agent-schema.json" -o inspectorAgentSchema.json
curl -sSf "${BASE}/inspector-module/inspector-collector/src/main/pinot/pinot-inspector-stat-application-realtime-table.json" -o inspectorApplicationTable.json
curl -sSf "${BASE}/inspector-module/inspector-collector/src/main/pinot/pinot-inspector-stat-application-schema.json" -o inspectorApplicationSchema.json

TABLES="uriStatTable.json tagTable.json doubleTable.json dataTypeTable.json exceptionTraceTable.json inspectorAgentTable.json inspectorApplicationTable.json"

# 다운받은 table json 은 localhost:19092 + multi-broker replicasPerPartition 가정. 단일 노드에 맞춤.
# shellcheck disable=SC2086
sed -i 's/localhost:19092/pinpoint-kafka:9092/g' $TABLES
# shellcheck disable=SC2086
sed -i 's/.*replicasPerPartition.*/    "replicasPerPartition": "1",/g' $TABLES

# pinot-admin.sh 가 exit code 0 을 항상 반환할 수 있어 (apache/pinot#7040) output 파싱이 필요.
# 정상: "successfully added" / 재기동 무해: "already exists" / 그 외: fail-fast.
for t in uriStat tag double dataType exceptionTrace inspectorAgent inspectorApplication; do
    output="$(/opt/pinot/bin/pinot-admin.sh AddTable \
        -schemaFile "${t}Schema.json" \
        -tableConfigFile "${t}Table.json" \
        -controllerHost pinot-controller \
        -controllerPort 9000 \
        -exec 2>&1)"
    status=$?
    if [ $status -eq 0 ] && echo "$output" | grep -q "successfully added"; then
        continue
    fi
    if echo "$output" | grep -qiE "already exists|TableAlreadyExist"; then
        echo "AddTable ${t}: already exists — skip"
        continue
    fi
    echo "AddTable ${t} 실패 (exit=$status):" >&2
    echo "$output" >&2
    exit 1
done
