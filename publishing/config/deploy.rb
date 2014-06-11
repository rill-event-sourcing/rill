set :application, 'my_app_name'
set :repo_url, 'git@github.com:StudyFlow/Gibbon.git'

# ask :branch, proc { `git rev-parse --abbrev-ref HEAD`.chomp }

set :deploy_to, '/rails'
# set :scm, :git

# set :format, :pretty
# set :log_level, :debug
# set :pty, true

# set :linked_files, %w{config/database.yml}
# set :linked_dirs, %w{bin log tmp/pids tmp/cache tmp/sockets vendor/bundle public/system}

# set :default_env, { path: "/opt/ruby/bin:$PATH" }
# set :keep_releases, 5

namespace :deploy do

  desc 'Restart application'
  task :restart do
    on roles(:app), in: :sequence, wait: 5 do
      # Your restart mechanism here, for example:
      # execute :touch, release_path.join('tmp/restart.txt')
    end
  end

  after :restart, :clear_cache do
    on roles(:web), in: :groups, limit: 3, wait: 10 do
      # Here we can do anything such as:
      # within release_path do
      #   execute :rake, 'cache:clear'
      # end
    end
  end

  # We have the Rails application in a subdirectory rails_app
  # Capistrano doesn't provide an elegant way to deal with that
  # for the git case. (For subversion it is straightforward.)
  task :mv_rails_app_dir do
    on roles(:app) do
      execute "mv #{release_path}/publishing/* #{release_path}/ "
    end
  end

  after :finishing, 'deploy:cleanup'
  after 'deploy:updating', 'deploy:mv_rails_app_dir'

end
