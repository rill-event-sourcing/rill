default: test

.PHONY: test default install


test:
	dropdb --if-exists rill_test
	createdb rill_test
	psql rill_test < psql_schema.sql
	PSQL_EVENT_STORE_URI="postgresql://localhost:5432/rill_test" lein test
	dropdb --if-exists rill_test

install:
	lein install
