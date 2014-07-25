set :rails_env, :staging
set :branch, :develop
server 'sfstagelearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{app}

