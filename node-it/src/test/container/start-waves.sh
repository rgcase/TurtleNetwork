#!/bin/bash
trap 'kill -TERM $PID' TERM INT
echo Options: $WAVES_OPTS
java $TN_OPTS -cp "/opt/TN/lib/*" com.wavesplatform.Application /opt/TN/template.conf &
PID=$!
wait $PID
trap - TERM INT
wait $PID
EXIT_STATUS=$?
