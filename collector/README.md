# Collector to read log from rollup and send them on a TCP port

The collector get data for the log in this format

AAAAAAAA 000AAAAA CCCCCCCC VVVVVVVV
LOW-Bit  High-Bit

with A representing an account on the roll-up,
C representing the color ('R','G','B' in ascii),
V representing the value of the pixel channel

## Run the collector
cargo run -- -f <logfile> -p <port>


## Run the docker

The docker read from the file given by `ENV FILE /tmp/log_file.txt` and write to `ENV PORT 1234`
make sure to set up these value accordingly when running the command.

`docker run -p 1234:<output_port> -v <dir_with_log_file>:/tmp/  collector`
