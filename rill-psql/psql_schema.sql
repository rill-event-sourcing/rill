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
