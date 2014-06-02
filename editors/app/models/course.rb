class Course < ActiveRecord::Base

  validates_presence_of :name

  def to_s
    "#{name}"
  end

end
