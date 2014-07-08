default: test

.PHONY: test default

lein:
	curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
	chmod 755 ./lein

test: lein
	./lein test && echo "OK"
