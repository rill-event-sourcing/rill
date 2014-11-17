class Reflection < ActiveRecord::Base
  belongs_to :section, touch: true

  validates :section, presence: true

  default_scope { order(:position) }

  after_create :set_position

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id, with_404 = true)
    reflections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if reflections.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple reflections found for uuid: #{id}") if reflections.length > 1
    reflections.first
  end

  def to_param
    "#{id[0,8]}"
  end

  def name
    "_REFLECTION_#{ position }_"
  end

private

  def set_position
    update_attribute(:position, section.increase_reflection_counter)
  end


end
