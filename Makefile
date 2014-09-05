default: test

.PHONY: test default install

test:
	dropdb rill_test 2> /dev/null || true
	createdb rill_test
	psql rill_test < psql_schema.sql
	PSQL_EVENT_STORE_URI="postgresql://localhost:5432/rill_test?user=studyflow&password=studyflow" lein test
	dropdb rill_test || true

install:
	lein install

clean:
	lein clean
