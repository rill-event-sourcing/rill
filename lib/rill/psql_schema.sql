CREATE TABLE rill_events (
       event_id VARCHAR(200), 
       stream_id VARCHAR(512),
       insert_order BIGSERIAL UNIQUE,
       stream_order BIGINT,
       payload BYTEA,
       UNIQUE(stream_id, stream_order)
);

CREATE INDEX stream_id_index ON rill_events (stream_id);

