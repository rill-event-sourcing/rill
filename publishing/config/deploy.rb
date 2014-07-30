set :application, 'my_app_name'
set :repo_url, 'git@gitlab.studyflow.nl:studyflow/gibbon.git'
set :deploy_to, '/rails'
set :log_level, :info

# set :linked_files, %w{config/database.yml}
# set :linked_dirs, %w{bin log tmp/pids tmp/cache tmp/sockets vendor/bundle public/system}
# set :default_env, { path: "/opt/ruby/bin:$PATH" }
# set :keep_releases, 5


desc 'Compile and deploy the application'
task :build_deploy => ["deploy:build_deploy"]


namespace :deploy do


  #############################################################################################
  # build and deploy staging branch

  desc 'deploy staging branch'
  task :build_deploy do
    run_locally do
      branch = capture("git rev-parse --abbrev-ref HEAD")
      info " -> running on branch: #{ branch }"
      if ['staging'].include?(branch)
        info " -> deploying branch: #{ branch }!"
        set :branch, branch
        last_commit = capture("git rev-parse HEAD")
        set :current_revision, last_commit
        info " -> deploying commit: #{ last_commit }!"
        info " -> start deploying..."
        invoke "deploy"
        info " -> done deploying"
      else
        info " NOT deploying branch: #{ branch }!"
      end
    end
  end


  #############################################################################################

  desc 'Restart application'
  task :restart do
    on roles(:app) do
      execute :touch, release_path.join('tmp/restart.txt')
    end
  end

  desc 'Compile assets'
  task :compile_assets => [:set_rails_env] do
    invoke 'deploy:assets:precompile'
  end

  # We have the Rails application in a subdirectory rails_app
  # Capistrano doesn't provide an elegant way to deal with that
  # for the git case. (For subversion it is straightforward.)
  task :mv_rails_app_dir do
    on roles(:app) do
      execute "rm -Rf /tmp/publishing"
      execute "mv #{release_path}/publishing /tmp/"
      execute "rm -Rf #{release_path}/*"
      execute "mv /tmp/publishing/* #{release_path}/"
      execute "rm -Rf /tmp/publishing"
    end
  end

  after :finishing, 'deploy:cleanup'
  after 'deploy:updating', 'deploy:mv_rails_app_dir'
  after 'deploy:updated', 'deploy:compile_assets'

  namespace :assets do
    task :precompile do
      on roles(:web) do
        within release_path do
          with rails_env: fetch(:rails_env) do
            execute :rake, "assets:precompile"
          end
        end
      end
    end
  end
end
