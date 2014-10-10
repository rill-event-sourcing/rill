#SHELL := /bin/bash

test:
	bundle install --jobs=2 --quiet --without development production
	RAILS_ENV=test bundle exec rake db:drop db:create db:migrate db:seed
	RAILS_ENV=test bundle exec rake exporter:export > /tmp/material.json
	RAILS_ENV=test bundle exec rake exporter:test_export
	bundle exec rspec

start:
	@if [ `gem list foreman -i` = true ]; then \
		echo "Foreman gem was already installed!"; \
	else \
		echo "Foreman gem not installed!"; \
		echo "  installing it now ;-)"; \
		gem install foreman; \
	fi
	@if [ -d "../../svgtex" ]; then \
		echo "Directory ../../svgtex found!"; \
	else \
		echo "Directory ../../svgtex NOT found!"; \
		echo "  installing it using:"; \
		echo "cd ../../ && git clone git@gitlab.studyflow.nl:studyflow/svgtex.git && cd -"; \
		exit 1; \
	fi
	foreman start

prepare:
	bundle exec rake db:drop db:create db:migrate db:seed

build:
	rm -f studyflow_publish.tar && \
	rm -Rf public/assets && \
	RAILS_ENV=production rake assets:clean assets:precompile && \
	tar -zcf studyflow_publish.tar * && \
	rm -Rf public/assets

upload:
	SHA=`git rev-parse HEAD` && \
	DATE=`date +'%Y%m%d-%H%M%S'` && \
	S3_FILE="s3://studyflow-server-images/$$SHA/$$DATE-studyflow_publish.tar" && \
	s3cmd --multipart-chunk-size-mb=5 put studyflow_publish.tar $$S3_FILE && \
	rm -f studyflow_publish.tar && \
	rm -Rf public/assets
