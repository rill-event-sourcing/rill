default: test

.PHONY: test default install

test:
	dropdb rill_test 2> /dev/null || true
	createdb rill_test
	psql rill_test < psql_schema.sql
	psql rill_test -c "GRANT ALL ON rill_events TO studyflow; GRANT ALL ON SEQUENCE rill_events_insert_order_seq TO studyflow;"
	PSQL_EVENT_STORE_URI="postgresql://localhost:5432/rill_test?user=studyflow&password=studyflow" lein test
	dropdb rill_test || true

install:
	lein install

clean:
	lein clean
