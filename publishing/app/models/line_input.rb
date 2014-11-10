class LineInput < Input

  has_many :answers

  def errors_when_publishing
    errors = []
    errors << "No correct answer for line input #{name} in #{inputable_type} '#{inputable.name}', in '#{inputable.parent}'" if answers.empty?
    errors << "Empty correct answer for line input #{name} in #{inputable_type} '#{inputable.name}', in '#{inputable.parent}'" unless answers.find_all{|answer| answer.value.blank?}.empty?
    errors
  end

  def to_publishing_format
    hash = {
      name: "_INPUT_#{position}_",
      correct_answers: answers.map(&:value),
      style: style
    }
    hash[:prefix] = prefix unless prefix.blank?
    hash[:suffix] = suffix unless suffix.blank?
    hash[:width]  = width  if width.to_i > 0
    hash
  end

end
