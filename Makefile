default: test

.PHONY: test build deploy install clean learning publishing school-administration login rill components css js

clean:
	cd lib/rill && lein clean
	cd lib/components && lein clean
	cd learning && lein clean
	cd login && lein clean
	cd school-administration && lein clean

install:
	cd lib/rill && lein install
	cd lib/components && lein install
	cd learning && lein install
	cd login && lein install
	cd school-administration && lein install

rill:
	cd lib/rill && make test && make install

components: rill
	cd lib/components && make test && make install

learning: rill components
	cd learning && make test && make build

login: rill components
	cd login && make test && make build

publishing:
	cd publishing && make test && make build

school-administration: rill components
	cd school-administration && make test && make build

build: login publishing learning school-administration


upload: build
	cd learning && make upload
	cd login && make upload
	cd publishing && make upload
	cd school-administration && make upload

css:
	make -C learning css

js:
	make -C learning js
