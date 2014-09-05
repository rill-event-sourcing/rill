class Question < ActiveRecord::Base
  include Trashable, Activateable

  before_save :set_default_text
  after_create :set_default_tools

  belongs_to :quizzable, polymorphic: true, touch: true
  acts_as_list scope: [:quizzable_id, :quizzable_type]

  has_many :inputs, as: :inputable
  has_many :line_inputs, as: :inputable
  has_many :multiple_choice_inputs, as: :inputable

  default_scope { order(:position, :name, :text) }


  scope :active, -> { where(active: true) }
  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  serialize :tools, Hash

  def self.find_by_uuid(id, with_404 = true)
    questions = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if questions.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple questions found for uuid: #{id}") if questions.length > 1
    questions.first
  end

  def to_s
    text.blank? ? "blank" : text
  end

  def parent
    quizzable
  end

  def to_param
    "#{id[0,8]}"
  end

  def set_default_text
    self.text ||= ""
    self.worked_out_answer ||= ""
  end

  def set_default_tools
    self.update_attribute(:tools, Tools.default)
  end

  def to_publishing_format_for_entry_quiz
    {
      id: id,
      text: render_latex_for_publishing(text, "question '#{name}', in '#{parent}'"),
      tools: tools.keys,
      line_input_fields: line_inputs.map(&:to_publishing_format),
      multiple_choice_input_fields: multiple_choice_inputs.map(&:to_publishing_format)
    }
  end

  def to_publishing_format_for_section
    hash = {
      id: id,
      text: render_latex_for_publishing(text, "question '#{name}', in '#{parent}'"),
      tools: tools.keys,
      line_input_fields: line_inputs.map(&:to_publishing_format),
      multiple_choice_input_fields: multiple_choice_inputs.map(&:to_publishing_format),
      worked_out_answer: worked_out_answer_with_default
    }
  end

  def made_worked_out_answer
    return nil if inputs.length > 1
    input =  inputs.first
    if input.is_a?(LineInput)
      value = input.answers.first.value
    elsif input.is_a?(MultipleChoiceInput)
      value = render_latex_for_publishing(input.choices.where(correct: true).first.value, "woa of question '#{name}', in '#{parent}'")
    end
    "Het juiste antwoord is: #{ value }"
  end

  def worked_out_answer_with_default
    worked_out_answer.blank? ? made_worked_out_answer : render_latex(worked_out_answer, "woa of question '#{name}', in '#{parent}'")
  end

  def inputs_referenced_exactly_once?
    inputs.find_all{|input| text.scan(input.name).length != 1}.empty?
  end

  def nonexisting_inputs_referenced?
    input_names = inputs.map(&:name)
    text.scan(/_INPUT_.*?_/).find_all{|match| !input_names.include? match}.any?
  end

  def errors_when_publishing_for_entry_quiz
    errors = []
    begin
      render_latex_for_publishing(text)
    rescue
      errors << "Errors in LaTeX rendering in section in question '#{name}', in '#{parent}'"
    end
    errors << "No Inputs on question '#{name}', in '#{parent}'" if inputs.count == 0
    errors << "Error in input referencing in question '#{name}', in '#{parent}'" unless inputs_referenced_exactly_once?
    errors << "Nonexisting inputs referenced in question '#{name}', in '#{parent}'" if nonexisting_inputs_referenced?
    errors << inputs.map(&:errors_when_publishing)
    errors.flatten
  end

  def errors_when_publishing
    errors = errors_when_publishing_for_entry_quiz
    errors << "No Worked-out-answer given for question '#{name}', in '#{parent}'" if inputs.count > 1 && worked_out_answer.blank?
    errors.flatten
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
