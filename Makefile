default: test

.PHONY: test

test:
	cd learning && make test
	cd publishing && make test
	cd login && make test
