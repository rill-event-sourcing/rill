# Module for manual exception mails and Slack notifications
require 'net/http'
require 'net/https'
require 'uri'
require 'yajl'

module Slack

  module Post

    DefaultOpts = {
      channel: '#general'
    }.freeze

    def self.post(message,chan=nil,opts={})
      raise "You need to call Slack::Post.configure before trying to send messages." unless configured?(chan.nil?)
      pkt = {
        channel: chan || config[:channel],
        text: message,
      }
      if config[:username]
        pkt[:username] = config[:username]
      end
      if opts.has_key?(:icon_url) or config.has_key?(:icon_url)
        pkt[:icon_url] = opts[:icon_url] || config[:icon_url]
      end
      if opts.has_key?(:icon_emoji) or config.has_key?(:icon_emoji)
        pkt[:icon_emoji] = opts[:icon_emoji] || config[:icon_emoji]
      end
      uri = URI.parse(post_url)
      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = true
      http.ssl_version = :TLSv1 # SSLv3 => FIX FOR Slack error on dropping SSL3
      http.verify_mode = OpenSSL::SSL::VERIFY_PEER
      req = Net::HTTP::Post.new(uri.request_uri)
      req.body = Yajl::Encoder.encode(pkt)
      req["Content-Type"] = 'application/json'
      resp = http.request(req)
      case resp
      when Net::HTTPSuccess
        return true
      else
        raise "There was an error while trying to post. Error was: #{resp.body}"
      end
    end

    def self.post_url
      "https://#{config[:subdomain]}.slack.com/services/hooks/incoming-webhook?token=#{config[:token]}"
    end

    NecessaryConfigParams = [:subdomain,:token].freeze

    def self.configured?(needs_channel=true)
      return false if needs_channel and !config[:channel]
      NecessaryConfigParams.all? do |parm|
        config[parm]
      end
    end

    def self.config
      @config ||= DefaultOpts
    end

    def self.configure(opts)
      @config = config.merge(prune(opts))
    end

    KnownConfigParams = [:username,:channel,:subdomain,:token,:icon_url,:icon_emoji].freeze

    def self.prune(opts)
      opts.inject({}) do |acc,(k,v)|
        k = k.to_sym
        if KnownConfigParams.include?(k)
          acc[k] = v
        end
        acc
      end
    end

  end


  class Notifier
    class << self

      def slack_text(build)
        "<" +
          Rails.application.routes.url_helpers.project_build_url(
                                                                 build.project, build,
                                                                 host: "gitlab-ci.studyflow.nl", protocol: 'https'
                                                                 ) +
          "|Build> of #{build.ref} <https://gitlab.studyflow.nl/studyflow/gibbon/commit/#{build.sha}|#{build.sha}> by #{build.git_author_name}"
      end

      def build_fail_slack_post(build)
        post("#{slack_text(build)} failed :poop:")
      end

      def build_success_slack_post(build)
        post("#{slack_text(build)} passed :banana:")
      end

      def post(str)
        Slack::Post.configure( subdomain: 'studyflow', token: 'QhbTT3kNGySH28UpOkF8oc40', username: 'HAL 9001' )
        Slack::Post.post str, "#notifications"
      end

    end
  end
end

################################################################################################################

namespace :slack_notify do
  desc "Send slack a deploy signal"
  task :deploy do
    begin
      stage  = fetch(:stage)
      user = ENV['USER'] || ENV['USERNAME']
      revision = fetch(:current_revision).to_s[0,8]

      msg = "A new version has been deployed for Gibbon (#{ stage })\n"
      msg += "version #{ revision } deployed by #{ user }"

      Slack::Post.configure( subdomain: 'studyflow', token: 'QhbTT3kNGySH28UpOkF8oc40', username: 'Capistrano' )
      Slack::Post.post msg, "#notifications"
    rescue Exception => ex
      p "Error on Slack notification: #{ ex }"
    end
  end
end

# in deploy do:
# after 'deploy:finished', 'slack_notify:deploy'

