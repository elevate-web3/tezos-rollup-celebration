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
		1 1 1234 1000

run-collector-docker :
	(trap 'kill 0' SIGINT; \
	for row in {0..19}; do \
		for col in {0..4}; do \
			i=$$((($$row)*5+($$col))); \
			port=$$((1200+$$i)); \
			docker run \
			--name collector$$i \
			--net=host collector:latest \
			$$row $$col $$port 1000 & \
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
		-v $(PWD)/configs:/tmp \
		-e CONFIG=/tmp/collectors-test10.json \
		--net=host \
		aggregator

kill-and-clean-docker :
	for i in {0..99}; do \
		docker kill collector$$i; \
		docker rm collector$$i; \
	done; \
	docker kill aggregator; \
	docker rm aggregator;
