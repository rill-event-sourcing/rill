class Chapter < ActiveRecord::Base
  belongs_to :course

  validates :course, :presence => true
  validates :name, :presence => true

  def to_s
    "#{name}"
  end

end
