set :application, "login"
set :repo_url, "git@gitlab.studyflow.nl:studyflow/gibbon.git"
set :deploy_to, '/home/studyflow/login'
set :log_level, :debug
set :keep_releases, 10
set :s3path, "s3://studyflow-server-images/login"
set :supervisor_name, "studyflow_login"
