# Branching model

We follow the main branching model detailed in [A successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model/).

Happy coding! :grinning:

### Master branch

The (protected) branch named `master` is in sync to the **production** version of the application. Deployment to production MUST always happen from this branch.

When this happens, a new **tag** is created, for ease of future reference.

Development directly on this branch should only happen for critical hotfixes and subsequent deployment.

### Development branch

The (protected) branch `develop` is the main development branch, which is kept in sync with **staging**.

* Merges from: `master` (for hotfixes)
* Merges back into: `master` (just before a deployment)

Development on this branch should only happen for single-commit fixes for which a feature branch and code-review are deemed not necessary (TO BE EVALUATED)

### Feature branches

Normal development happens in feature branches.

* Branches off: `develop`
* Merges back into: `develop`

Naming for this branches is not predetermined (except from the fact that it cannot start with either `develop-` or `master-`), but it should reasonably follow from the feature implemented.

When development is finished (and all test pass), a merge request is initiated, in order to perform a code review.

#### Merging a feature branch

In order to keep consistency with this branching model, all merges should be non-fast-forward, i.e. with

    git merge --no-ff <target branch>

in order to preserve the history of that feature branch.
