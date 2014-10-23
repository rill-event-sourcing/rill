class ChapterQuestionsSet < ActiveRecord::Base
  belongs_to :chapter_quiz
  acts_as_list scope: :chapter_quiz
  has_many :questions, as: :quizzable
  validates :chapter_quiz, presence: true

  default_scope { order(:position) }

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    chapter_questions_sets = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if chapter_questions_sets.empty? && with_404
    chapter_questions_sets.first
  end

  def to_s
    "#{name}"
  end

  def to_publishing_format
    {
      title: name,
      questions: questions.map(&:to_publishing_format_for_chapter_quiz)
    }
  end

  def to_param
    "#{id[0,8]}"
  end

end
