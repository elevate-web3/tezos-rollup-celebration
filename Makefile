.PHONY: all clean

all:
	dune build src/gen.exe src/visu.exe
	ln -sf _build/default/src/gen.exe
	ln -sf _build/default/src/visu.exe

clean:
	dune clean
	rm -fr gen.exe visu.exe
