(ns rill.event-store)

(defprotocol EventStore
  "The basic abstraction for storage and retrieval in rill.

  A backing store (or client code talking to a backing store, in a multi-process
  setup) only has to implement this basic protocol.

  Many transformation mechanisms (like message upcasting/migrating) can be
  specified using a middleware approach where the transforming code implements
  the EventStore protocol, wrapping around a lower-level EventStore instance to
  provide storage.

  Currently, rill Repositories take a hybrid approach in that they implement
  both EventStore using a wrapper strategy and extended it by implenting an
  additional Repository protocol."

  (retrieve-events-since
   [this stream-id cursor wait-for-seconds]
   "Returns a sequence of the events in stream `stream-id', starting from the
    given cursor. If there are no more events at that cursor, wait for
    `wait-for-seconds' to see if anything new shows up.

    `stream-id' may be stringified (using str) for comparison, so stream ids
    that stringify to the same value are assumed to describe the same stream.

    The initial cursor for retrieving a complete event stream is -1. See also
    `rill.event-store/retrieve-events'. Valid cursors for retrieval from a
    specific part of a stream can be achieved by calling `rill.message/cursor'
    on an earlier message in the stream, which will give a cursor useful for
    retrieving the next event onwards.

    The stream-id `rill.event-stream/all-events-stream-id' can be used for
    retrieving a read-only stream that contains all events in the event store.

    Note that the `wait-for-seconds' behaviour is intended to be used in
    applications where you're continuously listening for new events but don't
    want to overload the server with polls, and EventStores can implement the
    behaviour in whatever way is convenient technically, including just calling
    System/sleep before returning empy streams.")
  (append-events
   [this stream-id from-version events]
   "Appends `events' to stream `stream-id' at position `from-version'. Returns
    `true' if successful or `false' for failure.

    Events must be maps of printable/readable clojure data.

    EventStore implementations must make it impossible to append events in
    positions that are already taken, and should make it impossible to append
    events in any position except the one immediately following the last taken
    position. If multiple events are provided, all events in the chunk must be
    stored in the given order with no other events intermingled.

    It should be possible to append to multiple streams concurrently; this
    should not normally result in failure. Concurrent appends to the same stream
    may result in failure of at least one of the append-events calls.

    Retrieving events from a stream should eventually return all successfully
    committed events in the committed order, with `rill.message/number'
    providing the event's position. Failed appends should never result in
    retrievable events.

    The `from-version' to use for committing the first event in a stream is -1.
    Using -2 as the `from-version' will append the given events at whatever is
    the \"current\" position."))

(defn retrieve-events
  "Returns a sequence of all events in stream `stream-id' from the start. Will
  not wait for new events to arrive; if the stream is empty, returns an empty
  stream."
  [store stream-id]
  (retrieve-events-since store stream-id -1 0))

