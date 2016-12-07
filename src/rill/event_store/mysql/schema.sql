CREATE TABLE rill_events (
       stream_id VARCHAR(512) NOT NULL,
       insert_order BIGINT UNIQUE NOT NULL AUTO_INCREMENT,
       stream_order BIGINT NOT NULL,
       payload BLOB NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       event_type VARCHAR(512) NOT NULL,
       UNIQUE (stream_id, stream_order)
) ENGINE=MyISAM


