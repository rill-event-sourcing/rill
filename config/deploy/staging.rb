set :stage, :staging

server 'sfstagebalancer.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{balancer}
server 'sfstagelearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning}
server 'sfstagelogin.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login}
server 'sfstageschool.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school}
server 'sfstagepublish.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w(publish)
