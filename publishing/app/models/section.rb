class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  has_many :subsections, -> { order(:position) }

  acts_as_list :scope => :chapter

  has_many :subsections, -> { order(:position) }

  validates :chapter, :presence => true
  validates :title, :presence => true

  default_scope { order(:position) }

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  def self.find_by_uuid(id)
    sections = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if sections.empty?
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple sections found for uuid: #{id}") if sections.length > 1
    sections.first
  end

  def to_s
    "#{title}"
  end

  def as_json
    {
      id: id,
      title: title
    }
  end

  def to_param
    "#{id[0,8]}"
  end

end
