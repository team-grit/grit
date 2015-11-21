#!/bin/bash

SCRIPTDIR="${0%/*}";

cd "$SCRIPTDIR"

chmod +x bin/GRIT
#delete old output from older sessions
if [ -e log/grit.out ]
 then
  rm -f log/grit.out
  echo "old grit.old deleted"
fi

#log directory doesn't exist if grit was never run
if [ ! -e log ] || [ ! -d log ]
 then
  mkdir log
fi

#if grit.pid exist grit may already be running
if [ -e grit.pid ]
 then
  echo "Grit maybe already running, please execute shutdownGrit.sh first."
 else
  nohup bin/GRIT java -Xdebug -Xnoagent \ -Djava.compiler=NONE \ -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6001 &> log/grit.out &
  echo $! > grit.pid
fi
