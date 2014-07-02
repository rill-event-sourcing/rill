set :deploy_to, '/home/studyflow/login'
set :scm, :copy
set :log_level, :info
set :linked_files, %w{}
set :linked_dirs, %w{}
set :keep_releases, 5

namespace :deploy do

  desc 'Restart application'
  task :restart do
    on roles(:app) do
      execute "sudo supervisorctl restart studyflow_login"
    end
  end

  desc 'make JAR file'
  task :mk_jar_file do
    run_locally do
      execute "lein uberjar"
    end
  end

  desc 'upload lein-env file'
  task :upload_config do
    on roles(:app) do
      upload! ".lein-env", "#{ release_path }/.lein-env"
    end
  end

  before :updating, 'deploy:mk_jar_file'
  after :updating, 'deploy:upload_config'
  after :finishing, 'deploy:cleanup'
end

