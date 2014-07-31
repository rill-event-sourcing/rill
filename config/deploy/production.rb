set :stage, :production
set :branch, :master
server 'sfprodlogin.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{app}
