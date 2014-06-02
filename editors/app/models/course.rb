class Course < ActiveRecord::Base
  has_many :chapters

  validates :name, :presence => true

  attr_accessor :selected
  def self.current=(course_id)
    Thread.current[:course_id] = course_id
  end
  def self.current
    cur_id = Thread.current[:course_id].to_i
    cur_id > 0 ? find(cur_id) : nil
  end

  def to_s
    "#{name}"
  end

end
