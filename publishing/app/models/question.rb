class Question < ActiveRecord::Base
  include Trashable, Activateable

  validates :section, presence: true

  belongs_to :section, touch: true
  has_many :inputs, dependent: :destroy
  has_many :line_inputs
  has_many :multiple_choice_inputs

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

  # def as_json
  #   {
  #     id: id,
  #     title: title
  #   }
  #
  # end

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
