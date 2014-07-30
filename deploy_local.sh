#!/bin/bash

PATH=/home/studyflow/.rvm/gems/ruby-2.1.2/bin:/home/studyflow/.rvm/gems/ruby-2.1.2@global/bin:/home/studyflow/.rvm/rubies/ruby-2.1.2/bin:/home/studyflow/.rvm/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
source '/home/studyflow/.rvm/scripts/rvm'

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
echo "deploying JAVA...."

cd login                 && cap $1 deploy -s revision=$2 && cd -
cd learning              && cap $1 deploy -s revision=$2 && cd -
cd school-administration && cap $1 deploy -s revision=$2 && cd -

#################################################################
echo "deploying RAILS...."

if [ "$1" == "staging" ]
then
    BRANCH="develop"
    echo " -> branch: $BRANCH"
fi

if [ "$1" == "production" ]
then
    BRANCH="master"
    echo " -> branch: $BRANCH"
fi

cd publishing && cap $1 deploy -s branch=$BRANCH revision=$2 && cd -

#################################################################
echo "all deployed...."
