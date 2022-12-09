FROM archlinux:base-devel

# Setup pacman and update system
RUN pacman-key --init && pacman -Syu --noconfirm

# Install cargo
RUN pacman -S --noconfirm rustup \
	&& rustup toolchain install stable

# Copy project files
COPY . .

# build
RUN cargo build --release

# install
RUN mv target/release/collector /usr/local/bin/collector

# Run server
ENTRYPOINT ["/usr/local/bin/collector"]
