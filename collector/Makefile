rows = 2
cols = 25
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
	cargo run -- --log-path /tmp/rollup.txt --port 1234

run-n-dev-collectors:
	(trap 'kill 0' SIGINT; \
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			port=$$((1234+$$i)); \
			target/release/collector --log-path /tmp/rollup.txt --port $$port & \
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
			cargo run --release -- --log-path /tmp/logfile --port $$port && \
			echo "collector $i running" & \
		done; \
	done; \
	wait \
	)

build-prod-mockup:
	cargo build --release
