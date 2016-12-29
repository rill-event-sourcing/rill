CREATE TABLE rill_events (
       insert_order BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       stream_number BIGINT NOT NULL,
       stream_order BIGINT NOT NULL,
       payload BLOB NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       event_type VARCHAR(512) NOT NULL,
       INDEX stream_number_index (stream_number),
       INDEX stream_order_index (stream_order),
       CONSTRAINT UNIQUE stream_order_stream_number_constraint (stream_number, stream_order)
) ENGINE=MyISAM;

CREATE TABLE rill_streams (
       stream_number BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       stream_id VARCHAR(300) UNIQUE NOT NULL
) ENGINE=MyISAM;
