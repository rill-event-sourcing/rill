# login

A webapp to provide authentication for studyflow applications.

## Setup

sudo -u postgres createuser studyflow -W
sudo -u postgres createdb studyflow_login -O studyflow

## Usage

lein ring server
