CREATE TABLE rill_events (
       event_id VARCHAR(200), 
       stream_id VARCHAR(512),
       insert_order BIGSERIAL UNIQUE,
       stream_order BIGINT,
       payload TEXT,
       UNIQUE(stream_id, stream_order)
);

