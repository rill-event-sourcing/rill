default: test

.PHONY: test

test:
	cd learning && make test
	cd publishing && bundle exec rspec
