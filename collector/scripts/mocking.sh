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

COL_offset=$((COL*100))
ROW_offset=$((ROW*50))

INDEX=$(($ROW*$NB_COLS+$COL))
CROPED_FILE=""
shift 6
for image in $*; do
	CROPED_FILE+=" $image.croped$INDEX.ppm"
done

F=$(mktemp)
gen "$TPS" "all" "1" "1" $CROPED_FILE > "$F" &
echo -n 'Waiting for the log file to be created'
while [ ! -s "$F" ]; do sleep 1; echo -n '.'; done
echo 'done'
echo 'Running collector.'
collector --log-path $(cat "$F") --port "$PORT" --row "$ROW" --column "$COL"
