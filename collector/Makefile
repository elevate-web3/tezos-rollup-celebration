rows = 4
cols = 4
TPS = 1000

run-test :
	(trap 'kill 0' SIGINT; \
	for row in $(row_range); do \
		for col in $(col_range); do \
			i=$$((($$row)*$(cols)+($$col))); \
			port=$$((1200+$$i)); \
			./scripts/test.sh $$row $$col $$port $(TPS) & \
		done; \
	done; \
	)
