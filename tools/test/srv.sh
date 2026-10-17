#!/bin/bash
# Testserver steuern: srv.sh start|stop|fresh   (SRV=<Serverordner>, JAVA=<java-Binary>)
: "${SRV:?SRV setzen}" "${JAVA:?JAVA setzen}"
HERE=$(cd "$(dirname "$0")" && pwd)
PIDFILE=$SRV/server.pid

stop() {
  [ -f $PIDFILE ] || return 0
  pid=$(cat $PIDFILE)
  if kill -0 $pid 2>/dev/null; then
    python3 -I $HERE/rcon.py stop >/dev/null 2>&1
    for i in $(seq 1 60); do kill -0 $pid 2>/dev/null || break; sleep 2; done
    kill -0 $pid 2>/dev/null && kill $pid
  fi
  rm -f $PIDFILE
}

start() {
  cd $SRV
  rm -f console.log
  NF=$(ls libraries/net/neoforged/neoforge/)
  setsid $JAVA @user_jvm_args.txt @libraries/net/neoforged/neoforge/$NF/unix_args.txt nogui < /dev/null > console.log 2>&1 &
  echo $! > $PIDFILE
  for i in $(seq 1 120); do
    sleep 5
    grep -q -E 'Done \(|Crash report|Failed to start' console.log && break
    kill -0 $(cat $PIDFILE) 2>/dev/null || { echo "Server beendet"; break; }
  done
  grep -E 'Done \(|Crash report|Failed to start|effective worldgen' console.log
}

case "$1" in
  stop) stop ;;
  start) start ;;
  fresh) stop; rm -rf $SRV/world; start ;;
esac
