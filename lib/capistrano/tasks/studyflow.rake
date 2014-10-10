#########################################################################################################

desc 'Deploy application from S3'
task :deploy => ["deploy:hot"]

#########################################################################################################

desc 'Version of the deployed application'
task :version do
  [:stack_a, :stack_b].each do |stack|
    warn " #{ stack } ".center(72, "#")
    on roles *fetch(:release_roles), filter: stack do |host|
      role = host.roles.first
      if role == :publish
        target = release_path.join('REVISION')
      else
        symlink_dir = capture :readlink, "-f", release_path
        release_dir = capture :dirname, symlink_dir
        target = Pathname(release_dir).join('REVISION')
      end
      if test "[ -f #{target} ]"
        set(:previous_revision, capture(:cat, target, '2>/dev/null'))
      else
        set(:previous_revision, "UNKNOWN file #{target}")
      end
      warn " #{ host } #{ fetch(:previous_revision) } ".center(80, "#")
    end
  end
end

#########################################################################################################

namespace :deploy do

  desc 'Hot deploy application from S3'
  task :hot do
    Rake::Task["deploy:check"].execute
    Rake::Task["deploy:update"].execute

    Rake::Task["deploy:stack_a"].execute
    Rake::Task["deploy:update_stack"].execute
    Rake::Task["deploy:start_balancer"].execute

    Rake::Task["deploy:stack_b"].execute
    Rake::Task["deploy:update_stack"].execute
    Rake::Task["deploy:start_balancer"].execute

    Rake::Task["deploy:cleanup"].execute
    Rake::Task["deploy:finished"].execute
  end


  desc 'Cold deploy application from S3'
  task :cold do
    Rake::Task["deploy:check"].execute
    Rake::Task["deploy:update"].execute

    Rake::Task["deploy:stack_a"].execute
    Rake::Task["deploy:update_stack"].execute

    Rake::Task["deploy:stack_b"].execute
    Rake::Task["deploy:update_stack"].execute

    Rake::Task["deploy:stack_a"].execute
    Rake::Task["deploy:start_balancer"].execute

    Rake::Task["deploy:stack_b"].execute
    Rake::Task["deploy:start_balancer"].execute

    Rake::Task["deploy:cleanup"].execute
    Rake::Task["deploy:finished"].execute
  end


  desc "updating code on a stack"
  task :update_stack do
    throw " => Stack not set!" unless fetch(:stack)
    Rake::Task["deploy:stop_balancer"].execute

    Rake::Task["deploy:symlink"].execute
    Rake::Task["deploy:bundle_install"].execute
    Rake::Task["deploy:migrate"].execute
    Rake::Task["deploy:restart"].execute
    Rake::Task["deploy:delayed_jobs:restart"].execute

    run_locally do
      warn " DONE DEPLOYING ON STACK #{ fetch(:stack) } ".center(72, "#")
    end
  end


  desc "check for revisions and directories"
  task :check do
    throw "no valid GIT SHA given!" unless ENV['revision'] =~ /^[0-9a-f]{40}$/
    set :current_revision, ENV['revision']
    invoke 'deploy:check:directories'
    invoke 'deploy:check:linked_dirs'
  end


  desc "download code from S3 and unzip it"
  task :update do
    timestamp = Time.now.strftime("%Y%m%d%H%M%S")
    set(:release_timestamp, timestamp)
    set(:release_path, releases_path.join(timestamp))
    on roles *fetch(:release_roles) do |host|
      execute :mkdir, '-p', release_path
      within release_path do
        execute "s3cmd get #{ fetch(:s3path) }/#{ fetch(:current_revision) }/*#{ host.roles.first }* #{ release_path }/"
        execute "cd #{ release_path } && ls -r | sed 1d | while read i ; do echo \" -> deleting older release $i\" ; rm \"\$i\"; done"
      end
      role = host.roles.first
      if role == :publish
        within release_path do
          execute :tar, "-zxf", "*.tar"
          execute :rm, "-f", "*.tar"
        end
      end
    end
    run_locally do
      warn " NEW CODE DOWNLOADED ".center(72, "C")
    end
  end


  desc "set stack to stack A"
  task :stack_a do
    set :stack, :stack_a
    run_locally do
      warn " RUNNING stack A ".center(72, "#")
    end
  end


  desc "set stack to stack B"
  task :stack_b do
    set :stack, :stack_b
    run_locally do
      warn " RUNNING stack B ".center(72, "#")
    end
  end


  desc "set symlink to new version of application"
  task :symlink do
    on roles *fetch(:release_roles), filter: fetch(:stack) do |host|
      within deploy_path do
        execute :rm, '-rf', current_path
        role = host.roles.first
        if role == :publish
          execute :ln, '-s', release_path, current_path
          invoke 'deploy:symlink:linked_dirs'
        else
          jar_file = capture "ls #{ release_path }/*.jar"
          execute :ln, '-s', release_path.join(jar_file), current_path
        end
      end

      within release_path do
        execute :echo, "\"#{ fetch(:current_revision) }\" >> REVISION"
      end

      within deploy_path do
        revision_log_message = "Branch #{ fetch(:branch) } (at #{ fetch(:current_revision) }) deployed as release #{ fetch(:release_timestamp) } by #{ local_user }"
        execute %{echo "#{ revision_log_message }" >> #{ revision_log }}
      end
    end
  end


  desc "install gems for rails application"
  task :bundle_install do
    on roles :publish, filter: fetch(:stack) do |host|
      within release_path do
        warn " running bundle install on #{ host.hostname } ".center(72, "#")
        execute :bundle, :install, "--without='development test'"
      end
    end
  end


  desc "migrate the publishing database"
  task :migrate do
    on roles :db, filter: fetch(:stack) do |host|
      within release_path do
        warn " running migrations on #{ host.hostname } ".center(72, "#")
        execute :bundle, "exec rake db:migrate RAILS_ENV=#{ fetch(:stage) }"
      end
    end
  end


  desc "starting the servers"
  task :start do
    throw " => Stack not set!" unless fetch(:stack)
    on roles(*fetch(:release_roles), filter: fetch(:stack)) do |host|
      role = host.roles.first
      if role == :publish
        warn " starting publishing server #{ host } ".center(72, "#")
        execute :sudo, :apache2ctl, :start
      else
        warn " starting java server #{ host } ".center(72, "#")
        execute :sudo, :supervisorctl, :start, "studyflow_#{ role }"
      end
      check_up_server role
    end
  end


  desc "stopping the servers"
  task :stop do
    throw " => Stack not set!" unless fetch(:stack)
    on roles *fetch(:release_roles), filter: fetch(:stack) do |host|
      role = host.roles.first
      if role == :publish
        warn " stopping publishing server #{ host } ".center(72, "#")
        execute :sudo, :apache2ctl, :stop
      else
        warn " stopping java server #{ host } ".center(72, "#")
        execute :sudo, :supervisorctl, :stop, "studyflow_#{ role }"
      end
    end
  end


  desc "restarting the servers"
  task :restart do
    throw " => Stack not set!" unless fetch(:stack)
    on roles(*fetch(:release_roles), filter: fetch(:stack)) do |host|
      role = host.roles.first
      if role == :publish
        warn " restarting publishing server #{ host } ".center(72, "#")
        execute :touch, current_path.join("tmp", "restart.txt")
      else
        warn " restarting java server #{ host } ".center(72, "#")
#        execute :sudo, :supervisorctl, :stop, "studyflow_#{ role }"
        execute :sudo, :"/etc/init.d/supervisor", :stop
        execute :echo, " '##################### DEPLOY OF #{ fetch(:current_revision) } ON #{ fetch(:release_timestamp) } #########################################' | sudo tee -a /home/studyflow/#{ role }-stderr.log"
        execute :echo, " '##################### DEPLOY OF #{ fetch(:current_revision) } ON #{ fetch(:release_timestamp) } #########################################' | sudo tee -a /home/studyflow/#{ role }-stdout.log"
        sleep 2
        execute :sudo, :"/etc/init.d/supervisor", :start
#        execute :sudo, :supervisorctl, :start, "studyflow_#{ role }"
      end
      check_up_server role
    end
  end


  task :stop_balancer do
    throw " => Stack not set!" unless fetch(:stack)
    stack = fetch(:stack)
    on roles(:balancer) do
      (roles *fetch(:release_roles), filter: stack).each do |host|
        warn " disabling #{ host } on balancer ".center(72, "#")
        execute "sudo haproxyctl disable all #{ host }"
      end
      warn " disabled balancer for #{ stack } ".center(72, "#")
      warn " stopped #{ stack } ".center(72, "#")
    end
  end


  task :start_balancer do
    throw " => Stack not set!" unless fetch(:stack)
    stack = fetch(:stack)
    on roles(:balancer) do
      (roles *fetch(:release_roles), filter: stack).each do |host|
        warn " disabling #{ host } on balancer ".center(72, "#")
        execute "sudo haproxyctl enable all #{ host }"
      end
      warn " enabled balancer for #{ stack } ".center(72, "#")
      warn " started #{ stack } ".center(72, "#")
    end
  end


  desc 'Clean up old releases'
  task :cleanup do
    on roles *fetch(:release_roles) do |host|
      releases = capture(:ls, '-x', releases_path).split
      if releases.count >= fetch(:keep_releases)
        warn t(:keeping_releases, host: host.to_s, keep_releases: fetch(:keep_releases), releases: releases.count)
        directories = (releases - releases.last(fetch(:keep_releases)))
        if directories.any?
          directories_str = directories.map do |release|
            releases_path.join(release)
          end.join(" ")
          execute :rm, '-rf', directories_str
        else
          warn t(:no_old_releases, host: host.to_s, keep_releases: fetch(:keep_releases))
        end
      end
    end
  end


  task :finished do
    run_locally do
      warn " DONE ".center(72, "#")
    end
  end


  #########################################################################################################


  def check_up_server(role)
    port =   80 if role == :publish
    port = 3000 if role == :learning
    port = 4000 if role == :login
    port = 4001 if role == :teaching
    port = 5000 if role == :school
    throw "unknow port for role: #{ role }!" unless port

    on roles role, filter: fetch(:stack) do |host|
      warn " waiting for #{ role } application to be ready on #{ host }:#{ port }"
      load_time = 0
      status_up = false
      until status_up || load_time > fetch(:max_load_time)
        response = capture "curl -s --connect-timeout 1 'http://localhost:#{port}/health-check'; echo 'waiting for #{ host }...'"
        debug " response: #{ response }"
        status_up =(response =~ /{"status":"up"}/)
        warn " sleeping until #{ role } application is up on #{ host }:#{ port } (#{ load_time } seconds)"
        sleep 20
        load_time += 20
      end
      if load_time > fetch(:max_load_time)
        throw " #{ host } won't go up on #{ host }:#{ port }! ".center(72, "#")
      else
        warn " #{ host } is up on #{ host }:#{ port } ".center(72, "#")
      end
    end
  end


  #########################################################################################################


  namespace :delayed_jobs do
    desc "start delayed jobs on publishing"
    task :start do
      on roles :publish, filter: fetch(:stack) do |host|
        within release_path do
          with rails_env: fetch(:stage) do
            warn " starting delayed jobs on #{ host.hostname } ".center(72, "#")
            execute :bundle, :exec, 'bin/delayed_job', :start
          end
        end
      end
    end

    desc "restart delayed jobs on publishing"
    task :stop do
      on roles :publish, filter: fetch(:stack) do |host|
        within release_path do
          with rails_env: fetch(:stage) do
            warn " stopping delayed jobs on #{ host.hostname } ".center(72, "#")
            execute :bundle, :exec, 'bin/delayed_job', :stop
          end
        end
      end
    end

    desc "restart delayed jobs on publishing"
    task :restart do
      on roles :publish, filter: fetch(:stack) do |host|
        within release_path do
          with rails_env: fetch(:stage) do
            warn " restarting delayed jobs on #{ host.hostname } ".center(72, "#")
            execute :bundle, :exec, 'bin/delayed_job', :restart
          end
        end
      end
    end
  end

  #########################################################################################################


  namespace :check do
    desc "check dirs"
    task :directories do
      on roles *fetch(:release_roles) do
        execute :mkdir, '-pv', releases_path
      end
      on roles :publish do
        execute :mkdir, '-pv', shared_path
      end
    end

    desc "check linked dirs"
    task :linked_dirs do
      next unless any? :linked_dirs
      on roles :publish do
        execute :mkdir, '-pv', linked_dirs(shared_path)
      end
    end
  end



  namespace :symlink do
    desc 'Symlink linked directories'
    task :linked_dirs do
      next unless any? :linked_dirs
      on roles(:publish) do
        execute :mkdir, '-pv', linked_dir_parents(release_path)

        fetch(:linked_dirs).each do |dir|
          target = release_path.join(dir)
          source = shared_path.join(dir)
          unless test "[ -L #{target} ]"
            if test "[ -d #{target} ]"
              execute :rm, '-rf', target
            end
            execute :ln, '-s', source, target
          end
        end
      end
    end
  end

end # /namespace
