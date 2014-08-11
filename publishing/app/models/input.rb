class Input < ActiveRecord::Base
  belongs_to :inputable, polymorphic: true, touch: true

  default_scope { order(:position) }

  after_create :set_position

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id, with_404 = true)
    inputs = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if inputs.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple inputs found for uuid: #{id}") if inputs.length > 1
    inputs.first
  end

  def to_param
    "#{id[0,8]}"
  end

  def name
    "_INPUT_#{ position }_"
  end

  def line_input?
    is_a?(LineInput)
  end

  def multiple_choice_input?
    is_a?(MultipleChoiceInput)
  end

private

  def set_position
    update_attribute(:position, inputable.increase_max_position)
  end

end
