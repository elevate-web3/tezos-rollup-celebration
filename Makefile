run-collector-docker :
	(trap 'kill 0' SIGINT; \
	for i in {1200..1290}; do \
		docker run --name collector$$i --net=host collector:latest 1 1 123$$i 1000 & \
	done; \
	)

kill-and-kill-docker :
	for i in {1200..1290}; do \
		docker kill collector$$i; \
		docker rm collector$$i; \
	done; \


run-aggregator-docker :
	docker run \
		-v "${PWD}":/tmp \
		-e CONFIG=/tmp/collectors-test10.json \
		--net=host \
		aggregator
