default: test

.PHONY: test default deploy

test:
	STUDYFLOW_ENV=test lein test && echo "OK"

deploy
: test
	lein install && lein uberjar && cap production build
