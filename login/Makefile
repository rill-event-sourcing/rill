default: test

.PHONY: test default deploy

test:
	STUDYFLOW_ENV=test lein test && echo "OK"

deploy:
	cap staging build
