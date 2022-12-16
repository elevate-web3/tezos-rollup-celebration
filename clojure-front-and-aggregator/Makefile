run-aggregator:
	clojure -M:prod -m rollup.core --config=../config.json

run-aggregator-with-default-json-config:
	clojure -M:prod -m rollup.core --config=resources/collectors-example.json

download-deps:
	clojure -P -M:prod -m rollup.core

clean-cljs:
	rm -rf ./resources/public/cljs

build-prod-cljs: clean-cljs
	clojure -M:cljs-prod release frontend

open-browser-page:
	xdg-open http://localhost:9000
