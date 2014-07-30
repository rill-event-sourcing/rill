default: test

.PHONY: test deploy install clean

install:
	cd lib/rill && lein install
	cd lib/components && lein install
	cd learning && lein install
	cd login && lein install
	cd school-administration && lein install

clean:
	cd lib/rill && lein clean
	cd lib/components && lein clean
	cd learning && lein clean
	cd login && lein clean
	cd school-administration && lein clean


deploy: test
	cd learning && make deploy
	cd login && make deploy
	cd publishing && make deploy
	cd school-administration && make deploy

test:
	cd lib/rill && make test && make install
	cd lib/components && make test && make install
	cd learning && make test
	cd login && make test
	cd publishing && make test
	cd school-administration && make test

