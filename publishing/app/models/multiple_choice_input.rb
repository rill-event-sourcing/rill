class MultipleChoiceInput < Input

  has_many :choices

  def errors_when_publishing
    errors = []
    errors << "No choice for #{name} in question #{question_id[0,8]}" if choices.length < 1
    errors << "No correct choice for #{name} in question #{question_id[0,8]}" if choices.find_all{|choice| choice.correct?}.empty?
    errors << "Empty choice for #{name} in question #{question_id[0,8]}" if choices.find_all{|choice| choice.value.blank?}.any?
    errors
  end

  def to_publishing_format
    {
      name: "_INPUT_#{position}_",
      choices: choices.map(&:to_publishing_format)
    }
  end

end
