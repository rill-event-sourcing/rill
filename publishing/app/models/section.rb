class Section < ActiveRecord::Base
  include Trashable, Activateable

  belongs_to :chapter
  acts_as_list :scope => :chapter

  has_many :subsections, -> { order(:position) }

  validates :chapter, :presence => true
  validates :title, :presence => true

  default_scope { order(:position) }

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }

  # accepts_nested_attributes_for :subsections, allow_destroy: true

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

  def subsections=(subsection_hash)
    subsection_hash.each do |stars, new_subsections|
      new_subsections.values.each_with_index do |new_subsection, index|
        my_subsection = subsections.find(new_subsection['id'])
        my_subsection.update_attributes(
          title: new_subsection['title'],
          description: new_subsection['description'],
          position: index
        )
      end
    end
  end

end
