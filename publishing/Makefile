test:
	bundle install --jobs=2 --quiet
	bundle exec rake db:drop db:create db:migrate db:seed
	bundle exec rspec
	COURSE_NAME=Counting bundle exec rake exporter:export > tmp/material.json
