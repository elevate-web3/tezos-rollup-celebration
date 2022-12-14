#!/usr/bin/env bash

if [ "$#" -lt 4 ]; then
    echo "Usage: test.sh [row] [col] [port] [tps]";
    exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

"$SCRIPT_DIR"/mocking.sh "$1" "$2" 40 25 "$3" "$4" \
             /scoru-demo-quick-and-dirty/img/dogami.ppm \
             /scoru-demo-quick-and-dirty/img/objkt.ppm
