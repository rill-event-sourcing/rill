class ExtraExample < ActiveRecord::Base
  include HtmlParseable

  belongs_to :section, touch: true

  validates :section, presence: true

  default_scope { order(:position) }

  after_create :set_position

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id, with_404 = true)
    extra_examples = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if extra_examples.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple reflections found for uuid: #{id}") if extra_examples.length > 1
    extra_examples.first
  end

  def to_param
    "#{id[0,8]}"
  end

  def name
    "_EXTRA_EXAMPLE_#{ position }_"
  end

  def errors_when_publishing
    errors = []
    errors << "No content for #{name} in #{section.name}" if content.empty?
    errors
  end

  def to_publishing_format
    {
      name: name,
      title: title,
      default_open: default_open,
      content: render_latex_for_publishing(content, "extra_example '#{name}', in '#{section.name}'")
    }
  end


  private

  def set_position
    update_attribute(:position, section.increase_extra_example_counter)
  end


end
