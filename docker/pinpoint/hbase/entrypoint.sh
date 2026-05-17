#!/bin/sh
# pinpoint-hbase 3.0.5 image default CMD 의 `tail -f /dev/null` 트레일러를 대체. JVM 이 죽어도
# 컨테이너가 살아남는 문제 — 자세한 동기는 dev-infra.md "standalone HBase 의 트레이드오프".

set -e

# 호스트 단일 파일 bind mount 위에서는 configure-hbase.sh 의 sed -i 가 rename 단계에서
# "Device or resource busy" — 디렉토리 마운트로 받아 image 내부 위치로 cp 우회.
if [ -f /etc/pinpoint-hbase/hbase-create.hbase ]; then
    cp /etc/pinpoint-hbase/hbase-create.hbase /opt/hbase/hbase-create.hbase
fi

/usr/local/bin/initialize-hbase.sh

# image 에 ps/pgrep 부재 — /proc 직접 스캔으로 HBase JVM PID 추적.
sleep 10

JAVA_PID=""
for entry in /proc/*/comm; do
    pid="${entry#/proc/}"
    pid="${pid%/comm}"
    case "$pid" in
        *[!0-9]*) continue ;;
    esac
    name="$(cat "$entry" 2>/dev/null)" || continue
    if [ "$name" = "java" ]; then
        JAVA_PID="$pid"
        break
    fi
done

if [ -z "$JAVA_PID" ]; then
    echo "[entrypoint] HBase java process not found after init — container exiting for restart"
    exit 1
fi

echo "[entrypoint] HBase java PID=$JAVA_PID — watching for liveness"

while [ -e "/proc/$JAVA_PID" ]; do
    sleep 10
done

echo "[entrypoint] HBase java ($JAVA_PID) exited — container exiting for restart"
exit 1
