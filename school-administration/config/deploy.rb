set :application, "school"
set :repo_url, "git@gitlab.studyflow.nl:studyflow/gibbon.git"
set :deploy_to, '/home/studyflow/school'
set :log_level, :info
set :keep_releases, 10
set :s3path, "s3://studyflow-server-images/school"
set :supervisor_name, "studyflow_school"
