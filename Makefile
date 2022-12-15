run-10-collector-docker :
	(trap 'kill 0' SIGINT; \
	docker run --net=host collector:latest 1 1 1230 1000 &\
	docker run --net=host collector:latest 1 1 1231 1000 &\
	docker run --net=host collector:latest 1 1 1232 1000 &\
	docker run --net=host collector:latest 1 1 1233 1000 &\
	docker run --net=host collector:latest 1 1 1234 1000 &\
	docker run --net=host collector:latest 1 1 1235 1000 &\
	docker run --net=host collector:latest 1 1 1236 1000 &\
	docker run --net=host collector:latest 1 1 1237 1000 &\
	docker run --net=host collector:latest 1 1 1238 1000 &\
	docker run --net=host collector:latest 1 1 1239 1000 \
	)

run-aggregator-docker :
	docker run \
		-v "${PWD}":/tmp \
		-e CONFIG=/tmp/collectors-test10.json \
		--net=host \
		aggregator
