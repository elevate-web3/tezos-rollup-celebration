rows = 5
cols = 2
TPS = 5000

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
