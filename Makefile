default: test

.PHONY: test deploy

deploy:
	cd learning && make deploy
	cd login && make deploy
	cd school-administration && make deploy

test:
	cd lib/rill && make test && make install
	cd lib/components && make test && make install
	cd learning && make test
	cd publishing && make test
	cd login && make test
	cd school-administration && make test
