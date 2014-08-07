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

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    sections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if sections.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple sections found for uuid: #{id}") if sections.length > 1
    sections.first
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

  def to_publishing_format
    {
      id: id,
      title: title,
      subsections: subsections.map(&:to_publishing_format),
      questions: questions.active.map(&:to_publishing_format),
      inputs: inputs.map(&:to_publishing_format)
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
    errors << "Error in input referencing in section '#{name}', in '#{parent}'" unless inputs_referenced_exactly_once?
    errors << "Nonexisting inputs referenced in section '#{name}', in '#{parent}'" if nonexisting_inputs_referenced?
    errors << inputs.map(&:errors_when_publishing)
    errors << questions.active.map(&:errors_when_publishing)
    errors.flatten
  end


end
