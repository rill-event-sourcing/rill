set :stage, :production

server 'sfprodbalancer.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w{balancer}
server 'sfprodlearning1.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning},   stack_a: true
server 'sfprodlearning2.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning},   stack_b: true
server 'sfprodlogin1.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login},      stack_a: true
server 'sfprodlogin2.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login},      stack_b: true
server 'sfprodschool1.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school},     stack_a: true
server 'sfprodschool2.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school},     stack_b: true
server 'sfprodteaching1.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{teaching},   stack_a: true
server 'sfprodteaching2.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{teaching},   stack_b: true
server 'sfprodpublish.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{publish db}, stack_a: true
