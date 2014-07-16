default: test

.PHONY: test

deploy-staging:
	cd login && cap staging deploy
	cd publishing && cap staging deploy

test:
	cd lib/rill && make test && make install
	cd lib/components && make test && make install
	cd learning && make test
	cd publishing && make test
	cd login && make test

