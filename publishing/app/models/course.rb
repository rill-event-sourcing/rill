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


    errors = course.errors_when_publishing
    throw errors if errors.any?
    publish_response =  HTTParty.put(publishing_url,
                                     headers: { 'Content-Type' => 'application/json' },
                                     body: course_json,
                                     timeout: 10)

    #rescue Errno::ECONNREFUSED
    # failed_publish_msg = "Connection refused while publishing to: #{ publishing_url }"
    #rescue Net::ReadTimeout
    # failed_publish_msg = "Timeout while publishing to: #{ publishing_url }"
    #rescue Exception => ex
    # failed_publish_msg = "Unknow exception while publishing to: #{ publishing_url }: #{ ex }"
    #end
  end
  handle_asynchronously :publish!

end
