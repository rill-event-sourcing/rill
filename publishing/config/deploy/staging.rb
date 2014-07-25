set :stage, :staging
set :branch :develop
server 'sfstagepublish.studyflow.nl', port: 1022, user: 'studyflow', roles: %w(web app db)

