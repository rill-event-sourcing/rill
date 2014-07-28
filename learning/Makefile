default: test

.PHONY: test default deploy

test:
	lein test && echo "OK"

deploy:
	lein install && lein uberjar && cap production build
