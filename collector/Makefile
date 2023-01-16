rows = 2
cols = 5
TPS = 1130

row_bound := $(shell echo $$(($(rows) - 1)))
col_bound := $(shell echo $$(($(cols) - 1)))
row_range := $(shell seq 0 $(row_bound))
col_range := $(shell seq 0 $(col_bound))

run-test :
	(trap 'kill 0' SIGINT; \
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			port=$$((1200+$$i)); \
			./scripts/test.sh $$row $$col $$port $(TPS) & \
		done; \
	done; \
	wait \
	)

run-dev-collector:
	(trap 'kill 0' SIGINT; \
	scripts/mocking_random.sh 0 0 1234 & \
	wait )

run-n-collectors:
	(trap 'kill 0' SIGINT; \
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			port=$$((1234+$$i)); \
			scripts/mocking_random.sh $$row $$col $$port & \
		done; \
	done; \
	wait \
	)

run-25-random-collector:
	(trap 'kill 0' SIGINT; \
	for row in $(shell seq 0 4); do \
		for col in $(shell seq 0 4); do \
			i=$$((($$row)*5+($$col))); \
			port=$$((1200+$$i)); \
			scripts/mocking_random.sh $$row $$col $$port && \
			echo "collector $i running" & \
		done; \
	done; \
	wait \
	)

build-prod-mockup:
	cargo build --release
