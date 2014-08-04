set :stage, :staging

server 'sfstagelearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning}
server 'sfstagelogin.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login}
server 'sfstagepublish.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w(publish)
server 'sfstageschool.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school}
