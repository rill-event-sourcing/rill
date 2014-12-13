CREATE TABLE rill_events (
       event_id VARCHAR(200) NOT NULL, 
       stream_id VARCHAR(512) NOT NULL,
       insert_order BIGSERIAL UNIQUE NOT NULL,
       stream_order BIGINT NOT NULL,
       payload BYTEA NOT NULL,
       UNIQUE(stream_id, stream_order)
);

CREATE INDEX stream_id_index ON rill_events (stream_id);

