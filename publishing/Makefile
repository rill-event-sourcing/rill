test:
	bundle install --jobs=2 --quiet --without development production
	RAILS_ENV=test bundle exec rake db:drop db:create db:migrate db:seed
	RAILS_ENV=test bundle exec rake exporter:export > /tmp/material.json
	RAILS_ENV=test bundle exec rake exporter:test_export
	bundle exec rspec

prepare-db:
	bundle exec rake db:drop db:create db:migrate db:seed

build:
	rm -f studyflow_publish.tar && \
	bundle package --no-install && \
	bundle install --deployment --without="development test" && \
	tar -zcf studyflow_publish.tar *

upload:
	SHA=`git rev-parse HEAD` && \
	DATE=`date +'%Y%m%d-%H%M%S'` && \
	S3_FILE="s3://studyflow-server-images/$$SHA/$$DATE-studyflow_publish.tar" && \
	s3cmd --multipart-chunk-size-mb=5 put studyflow_publish.tar $$S3_FILE && \
	rm -f studyflow_publish.tar && \
	rm -Rf public/assets
