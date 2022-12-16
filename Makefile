rows = 5
cols = 2
TPS = 1000

row_bound := $(shell echo $$(($(rows) - 1)))
col_bound := $(shell echo $$(($(cols) - 1)))
row_range := $(shell seq 0 $(row_bound))
col_range := $(shell seq 0 $(col_bound))

build-collector-docker :
	docker build -t collector:latest -f Dockerfile.collector_and_mockup .

publish-collector-docker :
	docker tag collector:latest pewulfman/tezos-rollup-celebration:collector; \
	docker push pewulfman/tezos-rollup-celebration:collector

run-one-collector-docker :
	docker run \
		--name collector \
		--net=host \
		collector:latest \
		0 0 1200 1000

run-collector-docker :
	(trap 'kill 0' SIGINT; \
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			port=$$((1200+$$i)); \
			docker run \
			--name collector$$i \
			--net=host collector:latest \
			$$row $$col $$port $(TPS) & \
		done; \
	done; \
	)

build-aggregator-docker :
	docker build -t aggregator:latest -f Dockerfile.aggregator .

publish-aggregator-docker :
	docker tag aggregator:latest pewulfman/tezos-rollup-celebration:aggregator; \
	docker push pewulfman/tezos-rollup-celebration:aggregator

run-aggregator-docker :
	docker run \
		-v $(PWD):/tmp \
		-e CONFIG=/tmp/config.json \
		--net=host \
		aggregator

kill-and-clean-docker :
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			docker kill collector$$i; \
			docker rm collector$$i; \
		done; \
	done; \
	docker kill aggregator; \
	docker rm aggregator;
