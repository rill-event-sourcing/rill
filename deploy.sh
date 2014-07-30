#!/bin/bash

ssh gitlab-ci "cd gibbon && git pull && ./deploy_local.sh $1 $2"
