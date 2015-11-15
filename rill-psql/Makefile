.PHONY: test test-schema

test-schema:
	dropdb rill_test 2> /dev/null || true
	createdb rill_test
	psql rill_test < resources/rill_psql/psql_schema.sql

test: test-schema
	lein test
	dropdb rill_test || true
