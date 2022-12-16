#!/usr/bin/env bash

if [ "$#" -lt 4 ]; then
    echo "Usage: test.sh [row] [col] [port] [tps]";
    exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

"$SCRIPT_DIR"/mocking.sh "$1" "$2" 40 25 "$3" "$4" \
            "../scoru-demo-quick-and-dirty/img/2_Dogami_02.ppm" \
            "../scoru-demo-quick-and-dirty/img/3_objkt.ppm" \
            "../scoru-demo-quick-and-dirty/img/4_Tezotopia.ppm" \
            "../scoru-demo-quick-and-dirty/img/5_Fx(hash).ppm" \
            "../scoru-demo-quick-and-dirty/img/6_Neonz_01.ppm" \
            "../scoru-demo-quick-and-dirty/img/8_QuipuSwap.ppm" \
            "../scoru-demo-quick-and-dirty/img/9_TEIA.ppm" \
            "../scoru-demo-quick-and-dirty/img/10_Youves_01.ppm"
