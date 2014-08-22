default: test

.PHONY: test default deploy upload css

test:
	STUDYFLOW_ENV=test lein test && echo "OK"

css:
	cd sass && bundle install && bundle exec compass compile

build: css
	lein uberjar

upload:
	SHA=`git rev-parse HEAD` && \
	DATE=`date +'%Y%m%d-%H%M%S'` && \
	S3_FILE="s3://studyflow-server-images/$$SHA/$$DATE-studyflow_login.jar" && \
	s3cmd --multipart-chunk-size-mb=5 put target/*-SNAPSHOT-standalone.jar $$S3_FILE
