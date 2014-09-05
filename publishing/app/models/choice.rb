class Choice < ActiveRecord::Base

  belongs_to :multiple_choice_input

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    choices = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if choices.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple choices found for uuid: #{id}") if choices.length > 1
    choices.first
  end

  def to_publishing_format
    {
      value: render_latex(value, "choice"),
      correct: correct
    }
  end

  def to_param
    "#{id[0,8]}"
  end

end
