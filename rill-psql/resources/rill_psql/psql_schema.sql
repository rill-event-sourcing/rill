CREATE SEQUENCE rill_events_insert_order_seq;

CREATE TABLE rill_events (
       event_id VARCHAR(200) UNIQUE NOT NULL, 
       stream_id VARCHAR(512) NOT NULL,
       insert_order BIGINT UNIQUE,
       stream_order BIGINT NOT NULL,
       payload BYTEA NOT NULL,
       UNIQUE(stream_id, stream_order)
);

CREATE INDEX stream_id_index ON rill_events (stream_id);

ALTER SEQUENCE rill_events_insert_order_seq OWNED BY rill_events.insert_order;

CREATE FUNCTION rill_set_insert_order() RETURNS trigger AS $$
       DECLARE
        event RECORD;
       BEGIN
          -- ensure that insert order is generated in order of *visibility*
          PERFORM pg_advisory_xact_lock(3333, 'rill_events'::regclass::oid::integer);
          FOR event IN SELECT stream_id, stream_order FROM rill_events WHERE insert_order IS NULL ORDER BY stream_id ASC, stream_order ASC LOOP
            UPDATE rill_events SET insert_order = nextval('rill_events_insert_order_seq') WHERE stream_id = event.stream_id AND stream_order = event.stream_order;
          END LOOP;
          RETURN NULL;
       END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER rill_set_insert_order AFTER INSERT ON rill_events
       FOR EACH STATEMENT EXECUTE PROCEDURE rill_set_insert_order();

