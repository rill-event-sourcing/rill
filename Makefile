default: test

.PHONY: test build deploy install clean learning login publishing school-administration teaching rill components migrations css js

clean:
	cd lib/rill && lein clean
	cd lib/components && lein clean
	cd lib/migrations && lein clean
	cd learning && lein clean
	cd login && lein clean
	cd school-administration && lein clean
	cd teaching && lein clean

install:
	cd lib/rill && lein install
	cd lib/components && lein install
	cd lib/migrations && lein install
	cd learning && lein install
	cd login && lein install
	cd school-administration && lein install
	cd teaching && lein install


rill:
	cd lib/rill && make test && make install

components: rill
	cd lib/components && make test && make install

migrations: rill
	cd lib/migrations && make test && make install

learning: rill components migrations
	cd learning && make test && make build

login: rill components migrations
	cd login && make test && make build

publishing:
	cd publishing && make test && make build

school-administration: rill components migrations
	cd school-administration && make test && make build

teaching: rill components migrations
	cd teaching && make test && make build


build: learning login publishing school-administration teaching

test:	install
	cd lib/rill && make test
	cd lib/components && make test
	cd lib/migrations && make test
	cd learning && make test
	cd login && make test
	cd publishing && make test
	cd school-administration && make test
	cd teaching && make test

upload: build
	cd learning && make upload
	cd login && make upload
	cd publishing && make upload
	cd school-administration && make upload
	cd teaching && make upload

css:
	make -C learning css
	make -C login css

js:
	make -C learning js
