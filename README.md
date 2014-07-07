# Project Gibbon

> Let's go on a safari through the darkest jungle -- @joost

![](docs/Yi-Yuanji-Two-gibbons-in-an-oak-tree.jpg)


## Documentation

* [Branching model](docs/branching_model.md)
* [Commit messages](docs/commits.md)
## Testing

    make test

To test the publishing of the material from the publishing app, run (from the root folder)

    cd learning/server
    lein validate-course-material path/to/material.json

## Running learning/server test with local EventStore
    ATOM_EVENT_STORE_COMMAND="mono /home/mfex/studyflow/code/eventstore/EventStore.SingleNode.exe" lein test rill.event-store.atom-store-test

## Deploying to staging

**Make sure you are in the `develop` branch (not enforced for now) **

    make deploy-staging

