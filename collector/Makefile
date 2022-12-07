run-dev-collector-0:
	cargo run -- --log-path /tmp/rollup.txt --port 1234

run-dev-collector-1:
	cargo run -- --log-path /tmp/rollup.txt --port 1235

run-dev-collector-2:
	cargo run -- --log-path /tmp/rollup.txt --port 1236

run-dev-collector-3:
	cargo run -- --log-path /tmp/rollup.txt --port 1237

run-dev-collector-4:
	cargo run -- --log-path /tmp/rollup.txt --port 1238

run-dev-collector-5:
	cargo run -- --log-path /tmp/rollup.txt --port 1239

run-dev-collector-6:
	cargo run -- --log-path /tmp/rollup.txt --port 1240

run-dev-collector-7:
	cargo run -- --log-path /tmp/rollup.txt --port 1241

run-dev-collector-8:
	cargo run -- --log-path /tmp/rollup.txt --port 1242

run-dev-collector-9:
	cargo run -- --log-path /tmp/rollup.txt --port 1243

run-dev-collectors-all:
	(trap 'kill 0' SIGINT; \
	cargo run -- --log-path /tmp/rollup.txt --port 1234 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1235 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1236 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1237 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1238 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1239 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1240 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1241 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1242 &\
	cargo run -- --log-path /tmp/rollup.txt --port 1243 \
	)
