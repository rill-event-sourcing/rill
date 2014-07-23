set :stage, :staging

server 'sfstagepublish.studyflow.nl', port: 1022, user: 'studyflow', roles: %w(web app db)

