default: test

.PHONY: test default deploy

test:
	lein test && echo "OK"

deploy: test
	lein do cljsbuild clean, cljsbuild once prod && lein uberjar && lein install && cap production build
