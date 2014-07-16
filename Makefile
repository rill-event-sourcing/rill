default: test

.PHONY: test default install

lein:
	curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
	chmod 755 ./lein

test: lein
	./lein test

install: lein
	./lein install
