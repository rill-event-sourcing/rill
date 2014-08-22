default: test

.PHONY: test default deploy css js

test:
	lein test && echo "OK"

js:
	lein cljsbuild once

css:
	cd sass && bundle install && bundle exec compass compile

build: css
	lein cljsbuild clean && lein trampoline cljsbuild once prod && lein uberjar

upload:
	SHA=`git rev-parse HEAD` && \
	DATE=`date +'%Y%m%d-%H%M%S'` && \
	S3_FILE="s3://studyflow-server-images/$$SHA/$$DATE-studyflow_learning.jar" && \
	s3cmd --multipart-chunk-size-mb=5 put target/*-SNAPSHOT-standalone.jar $$S3_FILE
