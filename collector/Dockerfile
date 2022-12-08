FROM archlinux:base-devel

# Setup environment variables
ENV FILE ""
ENV PORT 1234


# Setup pacman and update system
RUN pacman-key --init && pacman -Syu --noconfirm

# Install cargo
RUN pacman -S --noconfirm rustup \
	&& rustup toolchain install stable

# Copy project files
COPY . .

# build
RUN cargo build --release

# Run server
CMD cargo run --release -- -f ${FILE} -p ${PORT}
