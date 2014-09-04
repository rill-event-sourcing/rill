# Branching model

We more or less follow the main branching model detailed in
[A successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model/),
except we don't use a `master` branch anymore.

Happy coding! :grinning:

### Development branch

The (protected) branch `develop` is the main development branch, which
_should_ be deployable.

## Releases

Normal releases to production are done from the `develop` `HEAD`.

### Release versions

Releases are tagged as `release-YYYY-MM-DD.X` where X is the
release number for that date starting with 1.

### Hotfixes

Hotfixes are branched off of the currently live release version. So to
make a hotfix checkout `release-....` tag and create a new branch for
the fix.

When the fix is ready, tag it using the normal release versioning tag
scheme and deploy. Then merge the hotfix code into `develop` `HEAD`.

### Feature branches

Normal development happens in feature branches.

* Branches off: `develop`
* Merges back into: `develop`

Naming for this branches is not predetermined (except from the fact
that it cannot start with either `develop-` or `master-`), but it
should reasonably follow from the feature implemented.

When development is finished (and all test pass), a merge request is
initiated, in order to perform a code review.

#### Merging a feature branch

In order to keep consistency with this branching model, all merges
should be non-fast-forward, i.e. with

    git merge --no-ff <target branch>

in order to preserve the history of that feature branch.
