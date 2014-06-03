class Course < ActiveRecord::Base
  include Trashable

  has_many :chapters

  validates :name, :presence => true, :uniqueness => true

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

end
