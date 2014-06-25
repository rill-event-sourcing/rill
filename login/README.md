# login

A webapp to provide authentication for Studyflow applications.

## Setup

    sudo -u postgres createuser studyflow -W
    sudo -u postgres createdb studyflow_login -O studyflow

or on OSX:
    createuser studyflow -W
    createdb studyflow_login -O studyflow

## Usage

    lein ring server
