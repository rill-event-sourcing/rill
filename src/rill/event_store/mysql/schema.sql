CREATE TABLE rill_events (
       stream_number BIGINT NOT NULL,
       insert_order BIGINT UNIQUE NOT NULL AUTO_INCREMENT,
       stream_order BIGINT NOT NULL,
       payload BLOB NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       event_type VARCHAR(512) NOT NULL,
       UNIQUE (stream_number, stream_order)
) ENGINE=MyISAM;

CREATE TABLE rill_streams (
       stream_id VARCHAR(300) UNIQUE NOT NULL,
       stream_number BIGINT UNIQUE NOT NULL AUTO_INCREMENT
) ENGINE=MyISAM;
