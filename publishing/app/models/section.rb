class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  acts_as_list :scope => :chapter

  validates :chapter, :presence => true
  validates :title, :presence => true

  default_scope { order(:position) }

  def to_s
    "#{title}"
  end
end
