test:
	bundle install --jobs=2 --quiet
	bundle exec rake db:setup
	bundle exec rspec
	COURSE_NAME=Math bundle exec rake exporter:export > material.json
