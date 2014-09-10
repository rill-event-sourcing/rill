class Course < ActiveRecord::Base
  include Trashable, Activateable

  has_many :chapters, -> { order(:position) }
  has_one :entry_quiz

  validates :name, presence: true, uniqueness: true

  default_scope { order(:name) }

  def self.current=(course)
    Thread.current[:course] = course
  end

  def self.current
    Thread.current[:course]
  end

  def to_s
    "#{name}"
  end

  def errors_when_publishing
    errors = chapters.active.map(&:errors_when_publishing).flatten
    errors << (entry_quiz ? entry_quiz.errors_when_publishing : "No entry quiz for the course")
    errors.flatten
  end

  def to_publishing_format
    {
      id: id,
      name: name,
      chapters: chapters.active.map(&:to_publishing_format),
      entry_quiz: (entry_quiz ? entry_quiz.to_publishing_format : nil)
    }
  end

  def publish!
    course_json = JSON.pretty_generate(self.to_publishing_format)
    publishing_url = "#{StudyflowPublishing::Application.config.learning_server}/api/internal/course/#{ id }"
    errors = errors_when_publishing
    throw errors if errors.any?

    begin
      publish_response =  HTTParty.put(publishing_url,
                                       headers: { 'Content-Type' => 'application/json' },
                                       body: course_json,
                                       timeout: 600)

    rescue Errno::ECONNREFUSED
      info = "Connection refused while publishing to: #{ publishing_url }"
      pretty_debug info
      throw info
    rescue Net::ReadTimeout
      info = "Timeout while publishing to: #{ publishing_url }"
      pretty_debug info
      throw info
    rescue Exception => ex
      info = "Unknown exception while publishing to: #{ publishing_url }: #{ ex }"
      pretty_debug info
      throw info
    end

    if !publish_response
      info = "Course '#{ self }' was NOT published! No response code!"
      pretty_debug info
      throw info
    end

    if publish_response.code != 200
      info = "Course '#{ self }' was NOT published! Response code was: #{ publish_response.code }"
      pretty_debug info
      throw info
    end

    pretty_debug "Course '#{ self }' was successfully published!"
  end

  handle_asynchronously :publish!

end
