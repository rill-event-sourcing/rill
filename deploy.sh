#!/bin/bash

# ssh gitlab-ci "cd gibbon && git pull && ./deploy_local.sh $1 $2"

if [ "$#" -ne 2 ]
then
    echo "Usage: $0 <environment> <full git sha>"
    exit 1
fi

if ! [[ $1 = "staging" || $1 = "production"] ]]
then
    echo "Usage: $0 <environment> <full git sha> (with environment: staging OR master!)"
    exit 1
fi

if ! [[ $2 =~ ^[a-f0-9]{40}$ ]]
then
    echo "Usage: $0 <environment> <FULL git sha> (so FULL sha, nothing else!)"
    exit 1
fi

#################################################################

cap $1 deploy -s revision=$2

#################################################################
echo "all deployed...."
