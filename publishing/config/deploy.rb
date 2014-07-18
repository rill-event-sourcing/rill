set :application, 'my_app_name'
# set :repo_url, 'git@github.com:StudyFlow/Gibbon.git'
set :repo_url, 'git@gitlab.studyflow.nl:studyflow/gibbon.git'

ask :branch, proc { `git rev-parse --abbrev-ref HEAD`.chomp }

set :deploy_to, '/rails'
# set :scm, :git

# set :format, :pretty
set :log_level, :info
# set :pty, true

# set :linked_files, %w{config/database.yml}
# set :linked_dirs, %w{bin log tmp/pids tmp/cache tmp/sockets vendor/bundle public/system}

# set :default_env, { path: "/opt/ruby/bin:$PATH" }
# set :keep_releases, 5

namespace :deploy do

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
