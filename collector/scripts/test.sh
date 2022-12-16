#!/usr/bin/env bash

if [ "$#" -lt 4 ]; then
    echo "Usage: test.sh [row] [col] [port] [tps]";
    exit 1
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

"$SCRIPT_DIR"/mocking.sh "$1" "$2" 40 25 "$3" "$4" \
            "../scoru-demo-quick-and-dirty/img/10. Youves 01.jpg" \
            "../scoru-demo-quick-and-dirty/img/2. Dogami 02.jpg" \
            "../scoru-demo-quick-and-dirty/img/3. objkt.jpg" \
            "../scoru-demo-quick-and-dirty/img/4. Tezotopia.jpg" \
            "../scoru-demo-quick-and-dirty/img/5. Fx(hash).jpg" \
            "../scoru-demo-quick-and-dirty/img/6. Neonz 01.jpg" \
            "../scoru-demo-quick-and-dirty/img/8. QuipuSwap.jpg" \
            "../scoru-demo-quick-and-dirty/img/9. TEIA.jpg" \
