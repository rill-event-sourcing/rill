default: test

.PHONY: test default deploy

test:
	lein test && echo "OK"

deploy: test
	lein install && lein do cljsbuild clean, cljsbuild once prod && lein uberjar && cap production build
