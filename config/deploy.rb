##########################################################
# Studyflow S3 deployment
##########################################################
set :deploy_to, "/home/studyflow/app"
set :s3path, "s3://studyflow-server-images"
set :max_load_time, 600
set :keep_releases, 10
set :release_roles, [:login, :learning, :school, :teaching, :publish]
set :rvm_roles, "publish"
set :log_level, :info
set :linked_dirs, ['log', 'tmp']

after 'deploy:finished', 'appsignal:deploy'
