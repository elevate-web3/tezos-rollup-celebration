#!/usr/bin/env bash

#
# This script assumes that "gen" from
# git@gitlab.com:yrg/scoru-demo-quick-and-dirty.git is in the PATH.
#

ROW="$1"
COL="$2"
PORT="$3"

F=$(mktemp)
S=$(mktemp)
rm $S
mockup_log $F $S &
echo -n 'Waiting for the log file to be created'
while [ ! -s "$F" ]; do sleep 1; echo -n '.'; done
echo 'done'
echo 'Running collector.'
collector --log-path "$F" -s "$S" --port "$PORT" --row "$ROW" --column "$COL"
