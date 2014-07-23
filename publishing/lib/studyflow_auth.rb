require "redis"

class StudyflowAuth
  def self.redis
    @redis ||= Redis.new(:host => StudyflowPublishing::Application.config.redis_server)
  end

  def self.logged_in?(user)
    throw "No connection to Redis!" unless redis
    redis.get(user)
    redis.expire(user, 21800)
  end
end
