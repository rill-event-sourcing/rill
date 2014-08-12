# Studyflow S3 deployment
set :deploy_to, "/home/studyflow/app"
set :s3path, "s3://studyflow-server-images"
set :max_load_time, 120
set :keep_releases, 10
set :log_level, :info
set :release_roles, [:login, :learning, :school, :publish]

#########################################################################################################

desc 'Deploy application from S3'
task :deploy => ["deploy:check", "deploy:update",
                 "deploy:stack_a", "deploy:stop_balancer", "deploy:symlink",
                 "deploy:bundle_install", "deploy:migrate", "deploy:restart", "deploy:check_up", "deploy:start_balancer",
                 "deploy:stack_b", "deploy:stop_balancer", "deploy:symlink",
                 "deploy:bundle_install",                   "deploy:restart", "deploy:check_up", "deploy:start_balancer",
                 "deploy:cleanup", "deploy:finished"]

#########################################################################################################

namespace :deploy do

  desc "check for revisions and directories"
  task :check do
    throw "no valid GIT SHA given!" unless ENV['revision'] =~ /^[0-9a-f]{40}$/
    set :current_revision, ENV['revision']
    on roles *fetch(:release_roles) do |host|
      execute :mkdir, '-pv', releases_path
    end
  end


  desc "download code from S3 and unzip it"
  task :update do
    timestamp = Time.now.strftime("%Y%m%d%H%M%S")
    set(:release_timestamp, timestamp)
    set(:release_path, releases_path.join(timestamp))
    on roles *fetch(:release_roles) do |host|
      execute :mkdir, '-p', release_path
      within release_path do
        execute "s3cmd get  #{ fetch(:s3path) }/#{ fetch(:current_revision) }/*#{ host.roles.first }* #{ release_path }/"
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
    p "ccccccccccccccccccccccccccccccccccccccccccccccccccc NEW CODE DOWNLOADED cccccccccccccccccccccccccccccccccccccccccccccccccccc"
  end


  task :stack_a do
    set :stack, :stack_a
    p " XXXXXXXXXXXXXXXXXXXXXX RUNNING ON stack A XXXXXXXXXXXXXXXXXXXXXX "
  end


  task :stack_b do
    set :stack, :stack_b
    p " XXXXXXXXXXXXXXXXXXXXXX RUNNING ON stack B XXXXXXXXXXXXXXXXXXXXXX "
  end


  task :stop_balancer do
    stack = fetch(:stack)
    on roles(:balancer) do
      (roles *fetch(:release_roles), filter: stack).each do |host|
        info "disabling #{ host.hostname } on balancer"
        execute "sudo haproxyctl disable all #{ host.hostname }"
      end
      info "disabled balancer for #{ stack }"
      info "waiting 2 seconds for traffic to stop to #{ stack }"
      sleep 2
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
    on roles(:publish), filter: fetch(:stack) do |host|
      within release_path do
        execute :bundle, :install, "--without='development test'"
      end
    end
  end


  desc "migrate the publishing database"
  task :migrate do
    on roles(:db) do |host|
      within release_path do
        execute :bundle, "exec rake db:migrate RAILS_ENV=#{ fetch(:stage) }"
      end
    end
  end


  desc "restarting the servers"
  task :restart do
    on roles *fetch(:release_roles), filter: fetch(:stack) do |host|
      info "restarting server: #{ host }"
      role = host.roles.first
      if role == :publish
        p "++++++++++++++++++++++++++++++++++++++++++++++++ restarting publishing server #{ host } ++++++++++++++++++++++++++++++++++++++++++++++++"
        execute :touch, current_path.join("tmp", "restart.txt")
      else
        p "++++++++++++++++++++++++++++++++++++++++++++++++ restarting java server #{ host } ++++++++++++++++++++++++++++++++++++++++++++++++"
        execute :sudo, :supervisorctl, :restart, "studyflow_#{ role }"
      end
    end
  end


  desc "checking status of the servers"
  task :check_up do
    check_up_server :learning, 3000
    check_up_server :login,    4000
    check_up_server :school,   5000
    check_up_server :publish,  80
  end


  task :start_balancer do
    stack = fetch(:stack)
    on roles(:balancer) do
      (roles *fetch(:release_roles), filter: stack).each do |host|
        info "disabling #{ host.hostname } on balancer"
        execute "sudo haproxyctl enable all #{ host.hostname }"
        info "waiting 2 seconds for traffic to start to #{ stack }"
        sleep 2
      end
      info "enabled balancer for #{ stack }"
    end
  end


  desc 'Clean up old releases'
  task :cleanup do
    on roles *fetch(:release_roles) do |host|
      releases = capture(:ls, '-x', releases_path).split
      if releases.count >= fetch(:keep_releases)
        info t(:keeping_releases, host: host.to_s, keep_releases: fetch(:keep_releases), releases: releases.count)
        directories = (releases - releases.last(fetch(:keep_releases)))
        if directories.any?
          directories_str = directories.map do |release|
            releases_path.join(release)
          end.join(" ")
          execute :rm, '-rf', directories_str
        else
          info t(:no_old_releases, host: host.to_s, keep_releases: fetch(:keep_releases))
        end
      end
    end
  end


  task :finished do
    p "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv DONE vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv"
  end


  def check_up_server(role, port)
    on roles role, filter: fetch(:stack) do |host|
      info "waiting for #{ role } application to be ready"
      load_time = 0
      status_up = false
      until status_up || load_time > fetch(:max_load_time)
        if role == :publish
          response = capture "curl -s --connect-timeout 1 'http://localhost/health-check'; echo 'waiting for #{ host }...'"
          status_up =(response =~ /{"status":"up"}/)
        else
          response = capture "curl -s --connect-timeout 1 -I 'http://localhost:#{ port }/'; echo 'waiting for #{ host }...'"
          status_up = (response =~ /HTTP\/1.1 200 OK/) || (response =~ /HTTP\/1.1 302 Found/)
        end
        info "sleeping until app is up (#{ load_time } seconds)"
        sleep 5
        load_time += 5
      end
      if load_time > fetch(:max_load_time)
        throw "#{ host } won't go up!"
      else
        info "#{ host } is up"
      end
    end
  end

end # /namespace


after 'deploy:finished', 'appsignal:set_version'
after 'deploy:finished', 'appsignal:deploy'
