set :stage, :production

server 'sfprodbalancer.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{balancer}

server 'sfprodlearning1.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning}
server 'sfprodlearning2.studyflow.nl', port: 1022, user: 'studyflow', roles: %w{learning}

server 'sfprodlogin1.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login}
server 'sfprodlogin2.studyflow.nl',    port: 1022, user: 'studyflow', roles: %w{login}

server 'sfprodschool1.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school}
server 'sfprodschool2.studyflow.nl',   port: 1022, user: 'studyflow', roles: %w{school}

server 'sfprodpublish.studyflow.nl',  port: 1022, user: 'studyflow', roles: %w(publish)
