class Course < ActiveRecord::Base
  has_many :chapters

  validates :name, :presence => true

  def to_s
    "#{name}"
  end

end
