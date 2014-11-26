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


## Calculator subtree
The folder lib/calculator is used as a git subtree from the branch
production_gibbon.
see: https://github.com/git/git/blob/master/contrib/subtree/git-subtree.txt

This means that the directory is both on Gibbon as on Rekenmachien.
Changes can be pushed to Gibbon and also to the Rekenmachien branch.

To pull from the subtree:
    git subtree pull --prefix lib/calculator git@gitlab.studyflow.nl:studyflow/rekenmachien.git production_gibbon
To push the subtree:
    git subtree push --prefix lib/calculator git@gitlab.studyflow.nl:studyflow/rekenmachien.git production_gibbon

The production_gibbon branch has a few tweaks to integrate the
calculator in Gibbon.
