set :stage, :production
set :branch, :master
server 'sfprodlearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{app}
