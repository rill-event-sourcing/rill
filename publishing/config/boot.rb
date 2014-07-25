# Set up gems listed in the Gemfile.
ENV['BUNDLE_GEMFILE'] ||= File.expand_path('../../Gemfile', __FILE__)

require 'bundler/setup' if File.exist?(ENV['BUNDLE_GEMFILE'])

require 'rails/commands/server'

module Rails
  class Server
    def default_options
      super.merge({
        :Port => 6000
      })
    end
  end
end
