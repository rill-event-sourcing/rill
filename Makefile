default: test

.PHONY: test deploy install clean

clean:
	cd lib/rill && lein clean
	cd lib/components && lein clean
	cd learning && lein clean
	cd login && lein clean
	cd school-administration && lein clean

install:
	cd lib/rill && lein install
	cd lib/components && lein install
	cd learning && lein install
	cd login && lein install
	cd school-administration && lein install

test:
	cd lib/rill && make test && make install
	cd lib/components && make test && make install
	cd learning && make test
	cd login && make test
	cd publishing && make test
	cd school-administration && make test

build:
	cd learning && make build
	cd login && make build
	cd publishing && make build
	cd school-administration && make build

upload: build
	cd learning && make upload
	cd login && make upload
	cd publishing && make upload
	cd school-administration && make upload

