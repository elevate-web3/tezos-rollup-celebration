#!/usr/bin/env bash

#
# This script assumes that "gen" from
# git@gitlab.com:yrg/scoru-demo-quick-and-dirty.git is in the PATH.
#

ROW="$1"
COL="$2"
NB_ROWS="$3"
NB_COLS="$4"
PORT="$5"
TPS="$6"

shift 6
F=$(mktemp)
gen "$TPS" "$ROW" "$COL" "$NB_ROWS" "$NB_COLS" $* > "$F" &
echo -n 'Waiting for the log file to be created'
while [ ! -s "$F" ]; do sleep 1; echo -n '.'; done
echo 'done'
echo 'Running collector.'
collector --log-path $(cat "$F") --port "$PORT" &
watch du -b "$F"
