class Subsection < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :section
  # acts_as_list :scope => [:section, :stars]

  validates :section, :presence => true
  # validates :title, :presence => true
  validates :stars, :presence => true

  default_scope { order(:position) }

  scope :find_by_star, ->(star) { where(stars: star)}
  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id, with_404 = true)
    subsections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if subsections.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple subsections found for uuid: #{id}") if subsections.length > 1
    subsections.first
  end

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
