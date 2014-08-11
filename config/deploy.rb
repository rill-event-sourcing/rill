# Studyflow S3 deployment
set :deploy_to, "/home/studyflow/app"
set :s3path, "s3://studyflow-server-images"
set :max_load_time, 120

#########################################################################################################

desc 'Deploy application from S3'
task :deploy => ["deploy:check", "deploy:update", "deploy:symlink", "deploy:restart"]

#########################################################################################################

namespace :deploy do

  desc "check for revisions and directories"
  task :check do
    throw "no valid GIT SHA given!" unless ENV['revision'] =~ /^[0-9a-f]{40}$/
    set :current_revision, ENV['revision']
    on release_roles(:all) do |host|
      execute :mkdir, '-pv', releases_path
    end
  end


  desc "download code from S3 and unzip it"
  task :update do
    timestamp = Time.now.strftime("%Y%m%d%H%M%S")
    set(:release_timestamp, timestamp)
    set(:release_path, releases_path.join(timestamp))

    on release_roles(:login, :learning, :school, :publish) do |host|
      execute :mkdir, '-p', release_path
      within release_path do
        execute "s3cmd get  #{ fetch(:s3path) }/#{ fetch(:current_revision) }/*#{ host.roles.first }* #{ release_path }/"
        execute "cd #{ release_path } && ls -r | sed 1d | while read i ; do echo \" -> deleting older release $i\" ; rm \"\$i\"; done"
      end
    end

    on release_roles(:publish) do |host|
      within release_path do
        execute :tar, "-zxf", "*.tar"
        execute :rm, "-f", "*.tar"
        execute :bundle, :install, "--without='development test'"
        invoke "deploy:migrate"
      end
    end
  end


  desc "migrate the publishing database"
  task :migrate do
    on release_roles(:publish) do |host|
      within release_path do
        execute :bundle, "exec rake db:migrate RAILS_ENV=#{ fetch(:stage) }"
      end
    end
  end


  desc "symlink the latest code"
  task :symlink do
    on release_roles(:login, :learning, :school, :publish) do |host|
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
    end
  end


  desc "restart the servers"
  task :restart do
    set :java_role, 'learning'
    set :java_port, 3000
    invoke "deploy:restart_java"

    set :java_role, 'login'
    set :java_port, 4000
    invoke "deploy:restart_java"

    set :java_role, 'school'
    set :java_port, 5000
    invoke "deploy:restart_java"

    invoke "deploy:restart_publish"
  end


  desc "restart a JAVA server"
  task :restart_java do
    on release_roles(fetch(:java_role)), in: :sequence, wait: 2 do |host|
      info "restarting #{ fetch(:java_role) } server: #{ host }"

      info "disabling on balancer"
      on release_roles(:balancer) do
        execute "sudo haproxyctl disable all #{ host }"
      end
      info "disabled balancer"

      info "waiting 2 seconds for traffic to stop"
      sleep 2

      info "restart the application to new version"
      execute :sudo, :supervisorctl, :restart, "studyflow_#{ fetch(:java_role) }"

      info "wait for application to be ready"
      load_time = 0
      status_up = false
      until status_up || load_time > fetch(:max_load_time)
        response = capture "curl -s --connect-timeout 1 'http://localhost:#{ fetch(:java_port) }/health-check'"
        status_up =(response =~ /{"status":"up"}/)
        info "sleeping until app is up (#{ load_time } seconden)"
        sleep 5
        load_time += 5
      end
      if load_time > fetch(:max_load_time)
        throw "#{ host } wont go up!"
      else
        info "#{ host } is up"
      end

      info "enabling on balancer"
      on release_roles(:balancer) do
        execute "sudo haproxyctl enable all #{ host }"
      end
      info "enabled on balancer"
    end
  end


  desc "restart the publish server"
  task :restart_publish do
    on release_roles(:publish) do |host|
      info "restarting login server: #{ host }"
      execute :touch, current_path.join("tmp", "restart.txt")

      info "wait for application to be ready"
      load_time = 0
      status_up = false
      until status_up || load_time > fetch(:max_load_time)
        response = capture "curl -s --connect-timeout 1 'http://localhost/health-check'"
        status_up =(response =~ /{"status":"up"}/)
        info "sleeping until app is up (#{ load_time } seconden)"
        sleep 5
        load_time += 5
      end
      if load_time > fetch(:max_load_time)
        throw "#{ host } wont go up!"
      else
        info "#{ host } is up"
      end
    end
  end

end # /namespace


def release_roles(*names)
  names << { exclude: :no_release } unless names.last.is_a? Hash
  roles(*names)
end


after 'deploy:restart', 'appsignal:set_version'
after 'deploy:restart', 'appsignal:deploy'
