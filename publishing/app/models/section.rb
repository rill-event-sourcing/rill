class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  acts_as_list scope: :chapter

  has_many :subsections, -> { order(:position) }
  has_many :questions, as: :quizzable

  has_many :inputs, as: :inputable
  has_many :line_inputs, as: :inputable

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

  def parse_errors
    errors = {}
    subsections.map do |subs|
      subs_err = subs.parse_errors
      if subs_err.any?
        errors["Subsection '#{subs}':"] = subs_err
      end
    end
    errors
  end

  def image_errors
    errors = {}
    subsections.map do |subs|
      subs_err = subs.image_errors
      if subs_err.any?
        errors["Subsection '#{subs}':"] = subs_err
      end
    end
    errors
  end

  def to_publishing_format
    {
      id: id,
      title: title,
      meijerink_criteria: meijerink_criteria,
      domains: domains,
      subsections: subsections.map(&:to_publishing_format),
      questions: questions.active.map(&:to_publishing_format_for_section),
      line_input_fields: line_inputs.map(&:to_publishing_format)
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

  def increase_max_position
    max_inputs if increment!(:max_inputs)
  end

  def inputs_referenced_exactly_once?
    full_text = subsections.map(&:text).join
    inputs.find_all{|input| full_text.scan(input.name).length != 1}.empty?
  end

  def nonexisting_inputs_referenced?
    input_names = inputs.map(&:name)
    full_text = subsections.map(&:text).join
    full_text.scan(/_INPUT_.*?_/).find_all{|match| !input_names.include? match}.any?
  end

  def errors_when_publishing
    errors = []
    errors << "No Meijerink criteria selected for section '#{name}'" if meijerink_criteria.empty?
    errors << "No domains selected for section '#{name}'" if domains.empty?
    errors << "Error in input referencing in section '#{name}', in '#{parent}'" unless inputs_referenced_exactly_once?
    errors << "Nonexisting inputs referenced in section '#{name}', in '#{parent}'" if nonexisting_inputs_referenced?
    errors << "No questions in section '#{name}', in '#{parent}'" if questions.active.empty?
    errors << "No subsections in section '#{name}', in '#{parent}'" if subsections.empty?
    errors << inputs.map(&:errors_when_publishing)
    errors << questions.active.map(&:errors_when_publishing)
    errors << subsections.map(&:errors_when_publishing)
    errors.flatten
  end

end
