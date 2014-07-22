class MultipleChoiceInput < Input

  has_many :choices

  def to_publishing_format
    {
      name: "_INPUT_#{position}_",
      choices: choices.map(&:to_publishing_format)
    }
  end

end
