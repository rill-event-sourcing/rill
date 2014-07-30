set :application, "learning"
set :repo_url, "git@gitlab.studyflow.nl:studyflow/gibbon.git"
set :deploy_to, '/home/studyflow/learning'
set :log_level, :info
set :keep_releases, 10
set :s3path, "s3://studyflow-server-images/learning"
set :supervisor_name, "studyflow_learning"
