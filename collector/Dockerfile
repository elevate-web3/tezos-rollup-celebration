FROM archlinux:base-devel

# Setup pacman and update system
RUN pacman-key --init && pacman -Syu --noconfirm

# Install cargo
RUN pacman -S --noconfirm rustup \
	&& rustup toolchain install stable

# install OPAM
RUN pacman --noconfirm -Su libx11 opam git && opam init -y && opam install -y graphics dune && eval $(opam env)

# Copy project files
COPY . .

# build
RUN cargo build --release

# install
RUN mv target/release/collector /usr/local/bin/collector

RUN eval $(opam env) && git clone https://gitlab.com/yrg/scoru-demo-quick-and-dirty.git && cd scoru-demo-quick-and-dirty && make && cp gen.exe /usr/local/bin/gen

RUN chmod a+rx scripts/*.sh

# Run server
ENTRYPOINT ["scripts/test.sh"]
