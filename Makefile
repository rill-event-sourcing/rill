default: test

.PHONY: test default install deploy

test:
	lein test

install:
	lein install

clean:
	lein clean

jar:
	lein jar

deploy:
	./deploy.sh
