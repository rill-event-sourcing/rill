default: test

.PHONY: test build deploy install clean  publishing rill components css js

clean:
	cd lib/rill && lein clean
	lein clean
	lein cljsbuild clean

rill:
	cd lib/rill && make install

publishing:
	cd publishing && make build

uberjars: css js rill
	lein with-profile login:learning:school-administration:teaching uberjar

build: publishing uberjars

test:
	cd lib/rill && make test
	cd publishing && make test
	lein test

upload: test build
	SHA=`git rev-parse HEAD` DATE=`date +'%Y%m%d-%H%M%S'`; for PROJECT in "teaching" "learning" "login" "school-administration"; do s3cmd --multipart-chunk-size-mb=5 put "target/$$PROJECT-standalone.jar" "s3://studyflow-server-images/$$SHA/$$DATE-studyflow_$$PROJECT.jar"; done

css:
	make -C learning css
	make -C login css

js:
	make -C learning js
