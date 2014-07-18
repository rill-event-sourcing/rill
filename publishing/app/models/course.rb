class Course < ActiveRecord::Base
  include Trashable, Activateable

  has_many :chapters, -> { order(:position) }
  has_many :questions, as: :quizzable

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

  def to_publishing_format
    {
      id: id,
      name: name,
      chapters: chapters.active.map(&:to_publishing_format)
    }
  end
end
