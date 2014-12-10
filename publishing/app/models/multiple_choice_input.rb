class MultipleChoiceInput < Input

  has_many :choices

  def errors_when_publishing
    errors = []
    if choices.length < 1
      errors << "No choice for #{name} in #{inputable_type} '#{inputable.name}' in '#{inputable.parent}'"
    else
      errors << "No correct choice for #{name} in #{inputable_type} '#{inputable.name}' in '#{inputable.parent}'" if choices.find_all{|choice| choice.correct?}.empty?
      errors << "Empty choice for #{name} in #{inputable_type} '#{inputable.name}' in '#{inputable.parent}'" if choices.find_all{|choice| choice.value.blank?}.any?
    end
    errors += choices.map(&:errors_when_publishing)
    errors.flatten
  end

  def to_publishing_format
    {
      name: "_INPUT_#{position}_",
      choices: choices.map(&:to_publishing_format)
    }
  end

end
