class MultipleChoiceInput < Input

  has_many :choices

  def as_json
    {
      name: "_INPUT_#{position}_",
      choices: choices.map(&:as_json)
    }
  end

end
