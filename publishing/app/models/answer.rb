class Answer < ActiveRecord::Base

  belongs_to :line_input

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    answers = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if answers.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple answers found for uuid: #{id}") if answers.length > 1
    answers.first
  end

  def to_param
    "#{id[0,8]}"
  end

end
