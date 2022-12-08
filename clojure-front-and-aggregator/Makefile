run-aggregator:
	clojure -M:prod -m rollup.core

run-aggregator-with-env-var:
	TEZOS_ROLLUP_CONF_PATH=resources/collectors.json clojure -M:prod -m rollup.core

download-deps:
	clojure -P -M:prod -m rollup.core

build-prod-cljs:
	rm -rf ./resources/public/cljs
	clojure -M:cljs-prod release frontend

open-browser-page:
	xdg-open http://localhost:9000
