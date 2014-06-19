class Input < ActiveRecord::Base
  belongs_to :question
  validates :question, presence: true

  def to_param
    "#{id[0,8]}"
  end

  def line_input?
    is_a?(LineInput)
  end

  def multiple_choice_input?
    is_a?(MultipleChoiceInput)
  end

end
