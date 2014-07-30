set :stage, :staging
set :branch, ENV['branch'] if ENV['branch']
ask :branch, :develop unless fetch(:branch)
server 'sfstagepublish.studyflow.nl', port: 1022, user: 'studyflow', roles: %w(web app db)

