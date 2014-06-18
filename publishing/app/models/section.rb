class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  acts_as_list :scope => :chapter

  has_many :subsections, -> { order(:position) }
  has_many :questions

  validates :chapter, :presence => true
  validates :title, :presence => true

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

  def as_json

    subsections_by_level = {"1_star" => [], "2_star" => [], "3_star" => []}

    subsections.each do |subsection|
      subsections_by_level["#{subsection.stars}_star"] << subsection.as_json
    end

    {
      id: id,
      title: title,
      subsections_by_level: subsections_by_level
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

end
