# Project Gibbon

> Let's go on a safari through the darkest jungle -- @joost

![](docs/Yi-Yuanji-Two-gibbons-in-an-oak-tree.jpg)


## Documentation

* [Branching model](docs/branching_model.md)
* [Commit messages](docs/commits.md)
* [Login flow](docs/login_flow.md)
## Testing

    make test

## Listening to events from the event store

    java -jar path/to/rill-or-uberjar.jar rill.cli \
         http://127.0.0.1:2113 admin changeit

## Deploying

push changes to gitlab and wait for CI to build the jars/zips.

    ./deploy [staging / production] [commit-sha]

See docs/branching_model.md
