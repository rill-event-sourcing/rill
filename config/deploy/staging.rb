set :stage, :staging
set :branch, :develop

server 'sfstagelearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learn}
server 'sfstagelogin.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login}
server 'sfstagepublish.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w(publish)
server 'sfstageschool.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school}
