set :stage, :staging

server 'publishing.beta.studyflow.nl', port: 1022, user: 'studyflow', roles: %w(web app db)

