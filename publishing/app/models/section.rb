class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  acts_as_list scope: :chapter

  has_many :subsections, -> { order(:position) }
  has_many :questions, as: :quizzable

  has_many :inputs, as: :inputable
  has_many :line_inputs, as: :inputable

  has_many :reflection_questions, foreign_key: :section_id, class_name: "Reflection"
  has_many :extra_examples

  validates :chapter, presence: true
  validates :title, presence: true

  default_scope { order(:position) }

  scope :active, -> { where(active: true) }

  serialize :meijerink_criteria, Array
  serialize :domains, Array

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    sections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if sections.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple sections found for uuid: #{id}") if sections.length > 1
    sections.first
  end

  def meijerink_criteria_hash=(input)
    self.meijerink_criteria = input.select{|k,v| v == "1"}.keys
  end

  def reflections
    self.reflection_questions
  end

  def meijerink_criteria_hash
    self.merijerink_criteria.map{|k| {k => "1"}}.reduce(&:merge)
  end

  def domains_hash=(input)
    self.domains = input.select{|k,v| v == "1"}.keys
  end

  def domains_hash
    self.domains.map{|k| {k => "1"}}.reduce(&:merge)
  end

  def to_s
    "#{title}"
  end

  def name
    title
  end

  def parent
    chapter
  end

  def parse_errors(attr)
    errors = {}
    subsections.map do |subs|
      subs_err = subs.parse_errors(attr)
      if subs_err.any?
        errors["Subsection '#{subs}':"] = subs_err
      end
    end
    reflections.map do |refl|
      refl_err = refl.parse_errors(:content) + refl.parse_errors(:answer)
      if refl_err.any?
        errors["Reflection '#{refl.name}':"] = refl_err
      end
    end
    extra_examples.map do |extra|
      extra_err = extra.parse_errors(:content)
      if extra_err.any?
        errors["Extra example '#{extra.name}':"] = extra_err
      end
    end
    errors
  end

  def image_errors(attr)
    errors = {}
    subsections.map do |subs|
      subs_err = subs.image_errors(attr)
      if subs_err.any?
        errors["Subsection '#{subs}':"] = subs_err
      end
    end
    reflections.map do |refl|
      refl_err = refl.image_errors(:content) + refl.image_errors(:answer)
      if refl_err.any?
        errors["Reflection '#{refl.name}':"] = refl_err
      end
    end
    extra_examples.map do |extra|
      extra_err = extra.image_errors(:content)
      if extra_err.any?
        errors["Extra example '#{extra.name}':"] = extra_err
      end
    end
    errors
  end

  def to_publishing_format
    {
      id: id,
      title: title.to_s.strip,
      meijerink_criteria: meijerink_criteria,
      domains: domains,
      subsections: subsections.map(&:to_publishing_format),
      reflections: reflections.map(&:to_publishing_format),
      questions: questions.active.map(&:to_publishing_format_for_section),
      line_input_fields: line_inputs.map(&:to_publishing_format),
      reflections: reflections.map(&:to_publishing_format),
      extra_examples: extra_examples.map(&:to_publishing_format)
    }
  end

  def as_full_json
    {
      id: id,
      title: title,
      description: description,
      updated_at: I18n.l(updated_at, format: :long)
    }
  end

  def to_param
    "#{id[0,8]}"
  end

  ##############################

  def increase_max_position
    max_inputs if increment!(:max_inputs)
  end

  def increase_reflection_counter
    reflection_counter if increment!(:reflection_counter)
  end

   def increase_extra_example_counter
    extra_example_counter if increment!(:extra_example_counter)
  end

  ##############################

  def inputs_referenced_exactly_once?
    full_text = subsections.map(&:text).join
    inputs.find_all{|input| full_text.to_s.scan(input.name).length != 1}.empty?
  end

  def nonexisting_inputs_referenced?
    input_names = inputs.map(&:name)
    full_text = subsections.map(&:text).join
    full_text.to_s.scan(/_INPUT_.*?_/).find_all{|match| !input_names.include? match}.any?
  end

  def nonexisting_reflections_referenced?
    reflection_names = reflections.map(&:name)
    full_text = subsections.map(&:text).join
    full_text.to_s.scan(/_REFLECTION_.*?_/).find_all{|match| !reflection_names.include? match}.any?
  end

  def nonexisting_extra_examples_referenced?
    extra_example_names = extra_examples.map(&:name)
    full_text = subsections.map(&:text).join
    full_text.to_s.scan(/_EXTRA_EXAMPLE_.*?_/).find_all{|match| !extra_example_names.include? match}.any?
  end

  ##############################

  def errors_when_publishing
    errors = []
    errors << "No Meijerink criteria selected for #{reference}" if meijerink_criteria.empty?
    errors << "No domains selected for #{reference}" if domains.empty?
    errors << "Error in input referencing in #{reference}" unless inputs_referenced_exactly_once?
    errors << "Nonexisting inputs referenced in #{reference}" if nonexisting_inputs_referenced?
    errors << "Nonexisting reflections referenced in #{reference}" if nonexisting_reflections_referenced?
    errors << "Nonexisting extra example referenced in #{reference}" if nonexisting_extra_examples_referenced?
    errors << "No questions in #{reference}" if questions.active.empty?
    errors << "No subsections in #{reference}" if subsections.empty?
    errors << extra_examples.map(&:errors_when_publishing)
    errors << inputs.map(&:errors_when_publishing)
    errors << questions.active.map(&:errors_when_publishing)
    errors << reflections.map(&:errors_when_publishing)
    errors << subsections.map(&:errors_when_publishing)
    errors.flatten
  end

  def reference
    "section '#{name}', in '#{parent}'"
  end

end
