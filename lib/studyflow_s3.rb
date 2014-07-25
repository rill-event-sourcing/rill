# Studyflow S3 deployment

Rake::Task["deploy:updating"].clear_actions

desc 'Compile and then upload application to S3'
task :build => ["deploy:build",
                "deploy:upload",
                "deploy:clean_s3"]

namespace :deploy do

  #############################################################################################
  # building

  desc 'Build application for deployment'
  task build: :new_release_path2 do
    run_locally do
      execute "mkdir -p s3output && rm -Rf s3output/*"
      execute "lein uberjar"
      current_revision = capture "git log -1 --format='%H'"
      execute "cp target/*-SNAPSHOT-standalone.jar s3output/#{ fetch(:release_file) }"
    end
  end

  desc 'Upload jar-file to S3'
  task upload: :new_release_path2 do
    run_locally do
      execute "s3cmd --multipart-chunk-size-mb=5 put s3output/#{ fetch(:release_file) } #{ fetch(:s3path) }/"
    end
  end

  desc 'Cleanup S3 output directory'
  task :clean_s3 do
    run_locally do
      execute "rm -Rf s3output"
    end
  end

  #############################################################################################
  # running

  desc 'Restart application'
  task :restart do
    on roles(:app) do
      execute "sudo supervisorctl restart #{ fetch(:supervisor_name) }"
    end
  end

  desc 'Update server(s) by setting up a new release.'
  task :updating => :new_release_path do
    invoke "deploy:set_current_revision2"
    invoke "deploy:create_release2"
  end

  desc "Place a REVISION file with the current revision SHA in the current release path"
  task :set_current_revision2 do
    on roles(:app) do
      within deploy_path do
        execute :echo, "\"#{fetch(:current_revision)}\" >> REVISION"
      end
    end
  end

  desc 'download application from S3'
  task :create_release2 do
    on roles(:app) do
      last_commit = capture("cd #{ repo_path } && git log -1 --format='%H'")
      ask :current_revision, last_commit
      set :release_file, "#{ fetch(:application) }-#{ fetch(:current_revision) }.jar"
      execute :mkdir, '-p', release_path
      execute "s3cmd get #{ fetch(:s3path) }/#{ fetch(:release_file) } #{ release_path }/"
    end
  end

  task :new_release_path2 do
    run_locally do
      ask :current_revision, capture("git log -1 --format='%H'")
      set :release_file, "#{ fetch(:application) }-#{ fetch(:current_revision) }.jar"
    end
  end

end
