class ChapterQuestionsSet < ActiveRecord::Base
  belongs_to :chapter_quiz
  acts_as_list scope: :chapter_quiz
  has_many :questions, as: :quizzable
  validates :chapter_quiz, presence: true

  default_scope { order(:position) }

  scope :for_short_uuid, ->(id) { where(["SUBSTRING(CAST(id AS VARCHAR), 1, 8) = ?", id]) }
  def self.find_by_uuid(id, with_404 = true)
    qs = for_short_uuid(id)
    raise ActiveRecord::RecordNotFound if qs.empty? && with_404
    raise StudyflowPublishing::ShortUuidDoubleError.new("Multiple sections found for uuid: #{id}") if qs.length > 1
    qs.first
  end

  def to_s
    "#{title}"
  end

  def to_publishing_format
    {
      id: id,
      title: title,
      questions: questions.map(&:to_publishing_format_for_chapter_quiz)
    }
  end

  def errors_when_publishing
    errors = []
    errors << "No questions in the chapter quiz for chapter '#{chapter_quiz.chapter.title}'" if questions.empty?
    errors << questions.map(&:errors_when_publishing_for_chapter_quiz)
    errors.flatten
  end

  def to_param
    "#{id[0,8]}"
  end

end
