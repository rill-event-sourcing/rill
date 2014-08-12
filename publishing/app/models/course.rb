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
    errors << "No entry quiz for the course" unless entry_quiz
  end


  def to_publishing_format
    {
      id: id,
      name: name,
      chapters: chapters.active.map(&:to_publishing_format),
      entry_quiz: (entry_quiz ? entry_quiz.to_publishing_format : nil)
    }
  end
end
