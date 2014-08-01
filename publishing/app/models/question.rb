class Question < ActiveRecord::Base
  include Trashable, Activateable

  before_save :set_default_text

  belongs_to :quizzable, polymorphic: true, touch: true

  has_many :inputs, dependent: :destroy
  has_many :line_inputs
  has_many :multiple_choice_inputs

  default_scope { order(:name, :text) }

  scope :active, -> { where(active: true) }
  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id, with_404 = true)
    questions = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if questions.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple questions found for uuid: #{id}") if questions.length > 1
    questions.first
  end

  def to_s
    text.blank? ? "blank" : text
  end

  def to_param
    "#{id[0,8]}"
  end

  def set_default_text
    self.text ||= ""
    self.worked_out_answer ||= ""
  end

  def to_publishing_format
    {
      id: id,
      text: render_latex(text),
      worked_out_answer: render_latex(worked_out_answer),
      line_input_fields: line_inputs.map(&:to_publishing_format),
      multiple_choice_input_fields: multiple_choice_inputs.map(&:to_publishing_format)
    }
  end

  def as_full_json
    {
      id: id,
      text: text,
      updated_at: I18n.l(updated_at, format: :long)
    }
  end

  def increase_max_position
    max_inputs if increment!(:max_inputs)
  end

end
