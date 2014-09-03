set :stage, :staging

server 'sfstagebalancer.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w{balancer}
server 'sfstagelearning1.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning},   stack_a: true
server 'sfstagelearning2.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning},   stack_b: true
server 'sfstagelogin1.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login},      stack_a: true
server 'sfstagelogin2.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login},      stack_b: true
server 'sfstageschool1.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school},     stack_a: true
server 'sfstageschool2.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school},     stack_b: true
server 'sfstageteaching1.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{teaching},   stack_a: true
server 'sfstageteaching2.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{teaching},   stack_b: true
server 'sfstagepublish.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{publish db}, stack_a: true
