default: test

.PHONY: test default js-dev js-prod

test:
	lein cljsbuild once test

js-dev:
	lein cljsbuild once

js-prod:
	lein cljsbuild once prod

clean:
	lein clean
