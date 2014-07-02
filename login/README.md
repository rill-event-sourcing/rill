# login

A webapp to provide authentication for Studyflow applications.

## Setup

### Creating the database

    sudo -u postgres createuser studyflow -W
    sudo -u postgres createdb studyflow_login -O studyflow

or on OSX:

    createuser studyflow -W
    createdb studyflow_login -O studyflow

### Seeding the database

`lein prepare-database`

## Usage

As the default environment in `.lein-env` is `development`, it is enough to specify

    lein ring server

If you want to run the server as another environment, prepend it as follows:

    STUDYFLOW_ENV=staging lein ring server
