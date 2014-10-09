default: test

.PHONY: test default deploy upload

test:
	STUDYFLOW_ENV=test lein test && echo "OK"

build:
	cd .. && lein install # install studyflow.jar locally
	lein uberjar

upload:
	SHA=`git rev-parse HEAD` && \
	DATE=`date +'%Y%m%d-%H%M%S'` && \
	S3_FILE="s3://studyflow-server-images/$$SHA/$$DATE-studyflow_reporting.jar" && \
	s3cmd --multipart-chunk-size-mb=5 put target/*-SNAPSHOT-standalone.jar $$S3_FILE
