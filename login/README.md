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

from `repl`

(use 'studyflow.login.main)
(bootstrap!)
(seed-database db)

## Usage

    lein ring server
