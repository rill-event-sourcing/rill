set :stage, :staging
set :branch, :develop
server 'sfstagelogin.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{app}

