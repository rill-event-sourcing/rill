require 'appsignal'
require 'appsignal/marker'

namespace :appsignal do
  desc "Set git version"
  task :set_version do
    on roles(:app) do
      within repo_path do
        set(:current_revision, capture(:git, "log -1 --format=%H"))
      end
    end
  end

  desc "Send appsignal a deploy signal"
  task :deploy do
    env  = fetch(:stage)
    user = ENV['USER'] || ENV['USERNAME']
    revision = fetch(:current_revision).to_s[0,8]

    logger = fetch(:logger, Logger.new($stdout))
    appsignal_config = Appsignal::Config.new(ENV['PWD'], env, fetch(:appsignal_config, {}), logger)

    if appsignal_config && appsignal_config.active?
      marker_data = {
        :revision   => revision,
        :user       => user
      }
      marker = Appsignal::Marker.new(marker_data, appsignal_config, logger)
      marker.transmit
    end
  end
end

# after 'deploy:restart', 'appsignal:set_version'
# after 'deploy:restart', 'appsignal:deploy'
