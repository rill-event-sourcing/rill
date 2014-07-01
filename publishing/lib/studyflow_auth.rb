require "redis"

class StudyflowAuth
  def self.redis
    Redis.new(:host => "login.studyflow.nl")
  end

  def self.logged_in?(user = 'a0407fe0-bb83-4963-a51f-3c9b2f09aae3')
    throw "No connection to Redis!" unless redis
    redis.get(user)
  end
end
