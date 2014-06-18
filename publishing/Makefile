test:
	RAILS_ENV=test bundle install --jobs=2 --quiet
	RAILS_ENV=test bundle exec rake db:drop db:create db:migrate db:seed
	RAILS_ENV=test bundle exec rspec
	RAILS_ENV=test COURSE_NAME=Counting bundle exec rake exporter:export > tmp/material.json
