class Chapter < ActiveRecord::Base
  belongs_to :course

  validates_presence_of :name

  def to_s
    "#{name}"
  end

end
