#!/bin/bash

SCRIPTDIR="${0%/*}";

cd "$SCRIPTDIR"

if [ -e grit.pid ]
 then
  kill -TERM $(cat grit.pid)
  rm -f grit.pid
 else
  echo "Grit wasn't running or there was an error"
fi