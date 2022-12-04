# Clojure aggregator

## Dependencies

You need:
- A JVM, preferably 17 but should work with version 8 minimum.
- The Clojure CLI: [https://clojure.org/guides/install_clojure](https://clojure.org/guides/install_clojure).
- A Rust toolchain (with `cargo`).

All those dependencies can be managed with the [asdf](https://asdf-vm.com/)
tool.

## Run the architecture locally

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
