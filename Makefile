default: test

.PHONY: test default install deploy

test:
	lein test
	lein install
	make -C rill-psql test

install:
	lein install
	cd rill-psql && lein install

clean:
	lein clean

jar:
	lein jar
	lein install
	cd rill-psql && lein jar

deploy:
	./deploy.sh
