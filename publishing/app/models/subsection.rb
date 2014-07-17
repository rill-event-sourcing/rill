class Subsection < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :section, touch: true

  validates :section, presence: true

  default_scope { order(:position) }

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

  def to_publishing_format
    {
      id: id,
      title: title,
      text: text
    }
  end

  def as_full_json
    {
      id: id,
      position: position,
      title: title,
      text: text
    }
  end

  def to_param
    "#{id[0,8]}"
  end
end
