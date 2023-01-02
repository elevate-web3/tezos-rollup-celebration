# Clojure aggregator

## Dependencies

You need:
- A JVM, preferably 17 but should work with version 8 minimum.
- The Clojure CLI: [https://clojure.org/guides/install_clojure](https://clojure.org/guides/install_clojure).
- A Rust toolchain (with `cargo`).

All those dependencies can be managed with the [asdf](https://asdf-vm.com/)
tool.


## Run the architecture locally with the JVM mockup

### 1. Compile the ClojureScript code

```bash
cd PROJECT_ROOT/clojure-front-and-aggregator
make build-prod-cljs
```

### 2. Run the mockup

```bash
cd PROJECT_ROOT/clojure-front-and-aggregator
clojure -M:prod -m rollup.core --stream-mockup=random --rows=20 --columns=25 --interval=40 --msg-size=40
```


## Run the architecture locally with the Rust mockup

### 1. Compile the ClojureScript code

```bash
cd PROJECT_ROOT/clojure-front-and-aggregator
make build-prod-cljs
```

### 2. Run the mockup

```bash
cd PROJECT_ROOT/mockup_log
make run-dev-rollup-log
```

### 3. Run the collector

```bash
cd PROJECT_ROOT/collector
make run-dev-collector
```

### 4. Run the aggregator

```bash
cd PROJECT_ROOT/clojure-front-and-aggregator
make run-aggregator
```

### 5. Open http://localhost:9000

Or run:

```bash
make open-browser-page
```

## Run the docker

The aggregator use a JSON file to configure the address + port of the collector to listen to specified in `ENV CONFIG` and open a web-server on port 9000

`docker run -p 9000:80 -e CONFIG:<CONTAINER_FOLDER/CONFIG_FILE> -v <HOST_FOLDER>:<CONTAINER_FOLDER> aggregator
