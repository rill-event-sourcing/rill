class Question < ActiveRecord::Base
  include Trashable, Activateable

  validates :section, presence: true

  belongs_to :section, touch: true
  has_many :inputs, dependent: :destroy

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    questions = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if questions.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple questions found for uuid: #{id}") if questions.length > 1
    questions.first
  end

  def to_s
    "#{text}"
  end

  def to_param
    "#{id[0,8]}"
  end

end
