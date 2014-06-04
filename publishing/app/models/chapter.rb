class Chapter < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :course
  has_many :sections, -> { order(:position) }

  acts_as_list :scope => :course

  validates :course, :presence => true
  validates :title, :presence => true

  default_scope { order(:position) }

  def to_s
    "#{title}"
  end

end
