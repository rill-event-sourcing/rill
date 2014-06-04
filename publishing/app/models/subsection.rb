class Subsection < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :section
  acts_as_list :scope => :section

  validates :section, :presence => true
  validates :title, :presence => true
  validates :level, :presence => true

  default_scope { order(:position) }
  scope :find_by_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]).first }

  def to_s
    "#{title}"
  end

  def as_json
    {
      id: id,
      title: title
    }
  end

  def to_param
    "#{id[0,8]}"
  end
end
