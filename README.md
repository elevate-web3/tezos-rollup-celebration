# Tezos Rollup Celebration

## Run the expertiment

build the dockers :
```
make build-collector-docker
make build-aggregator-docker
```

Setup the parameter in the Makefile

generate config file :
```
gen_config.py -r <row> -c <col>
```

run the collectors :
```
make run-collector-docker
```

run the aggregator :
```
make run-aggregator :
```

open you browser and use the address "localhost:9000"

## Doc

https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API/Tutorial/Pixel_manipulation_with_canvas

## Installation

Node, yarn, clojure CLI and JVM required.

```
yarn install
```

## Dev

From Emacs, run `cider-jack-in-cljs`. Then select `shadow` and `frontend`.

Let's REPL!
