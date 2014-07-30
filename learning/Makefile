default: test

.PHONY: test default deploy

test:
	lein test && echo "OK"

deploy: test
	lein cljsbuild clean && lein trampoline cljsbuild once prod && lein uberjar && lein install && cap staging build_deploy
