default: test

.PHONY: test

deploy-staging:
	cd login && cap staging deploy
	cd publishing && cap staging deploy

test:
	cd learning && make test
	cd publishing && make test
	cd login && make test
