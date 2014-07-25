default: test

.PHONY: test default deploy

test:
	lein test && echo "OK"

deploy:
	cap staging build
