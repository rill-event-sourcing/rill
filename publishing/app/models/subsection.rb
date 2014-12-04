class Subsection < ActiveRecord::Base
  include Trashable, Activateable, HtmlParseable

  belongs_to :section, touch: true

  validates :section, presence: true

  #acts_as_list scope: :section
  default_scope { order(:position) }

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    subsections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if subsections.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple subsections found for uuid: #{id}") if subsections.length > 1
    subsections.first
  end

  def to_s
    "#{title}"
  end

  def to_publishing_format
    {
      id: id,
      title: title.to_s.strip,
      text: preparse_text_for_publishing(text, "section '#{section.name}', in '#{section.parent}'")
    }
  end

  def as_full_json
    {
      id: id,
      position: position,
      title: title,
      text: text
    }
  end

  def to_param
    "#{id[0,8]}"
  end

  def errors_when_publishing
    errors = []
    begin
      preparse_text_for_publishing(text)
    rescue
      errors << "Errors in LaTeX rendering in section '#{section.name}', in '#{section.parent}'"
    end
    errors << "No content in subsection of section '#{section.name}', in '#{section.parent}'" if text.blank?
    errors += image_errors(:text)
    errors
  end

  def reference
    "subsection of section '#{section.name}', in '#{section.parent}'"
  end

end
