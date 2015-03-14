-- This updates all non-unique event_ids to a fresh random generated
-- uuid.

-- Some unreleased versions of rill did not generate message ids
-- correctly and before 0.2.0 the database schema did not enforce
-- uniqueness so you may need this.

-- On ubuntu, you need to have "postgresql-contrib" installed to load
-- the extension

CREATE EXTENSION "uuid-ossp";

-- to see if there are any duplicates run:
-- SELECT event_id, COUNT(1) FROM rill_events GROUP BY event_id HAVING COUNT(1) > 1;
-- or
-- SELECT stream_id, stream_order FROM (SELECT stream_id, stream_order, event_id, COUNT(*) OVER (PARTITION BY event_id) AS cnt FROM rill_events) sub WHERE cnt > 1;

UPDATE rill_events SET event_id = uuid_generate_v4() FROM (SELECT stream_id, stream_order, event_id, COUNT(*) OVER (PARTITION BY event_id) AS cnt FROM rill_events) AS sub WHERE sub.cnt > 1 AND sub.stream_id = rill_events.stream_id AND sub.stream_order = rill_events.stream_order;



-- LIMIT TO FIRST EVENTS
UPDATE rill_events SET event_id = uuid_generate_v4() FROM (SELECT stream_id, stream_order, event_id, COUNT(*) OVER (PARTITION BY event_id) AS cnt FROM rill_events WHERE insert_order < 150000) AS sub WHERE sub.cnt > 1 AND sub.stream_id = rill_events.stream_id AND sub.stream_order = rill_events.stream_order;




