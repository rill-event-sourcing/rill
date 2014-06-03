class Chapter < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :course
  validates :course, :presence => true
  validates :title, :presence => true

  def to_s
    "#{title}"
  end

end
