set :stage, :staging

server 'sfstagebalancer.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{balancer}
server 'sfstagelearning.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning},   stack_a: true
server 'sfstagelogin.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login},      stack_a: true
server 'sfstageschool.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school},     stack_a: true
server 'sfstagepublish.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w{publish db}, stack_a: true
