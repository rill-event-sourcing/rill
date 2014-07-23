set :stage, :production
#set :branch, :master

server 'sfprodpublish.studyflow.nl', port: 1022, user: 'studyflow', roles: %w(web app db)
