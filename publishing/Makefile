test:
	bundle install --jobs=2 --quiet --without development production
	RAILS_ENV=test bundle exec rake exporter:export > /tmp/material.json
	RAILS_ENV=test bundle exec rake exporter:test_export
	RAILS_ENV=test bundle exec rake db:drop db:create db:migrate db:seed
	bundle exec rspec

prepare-db:
	bundle exec rake db:drop db:create db:migrate db:seed
