class MultipleChoiceInput < Input

  has_many :choices

  def as_json
    {
      id: id,
      choices: choices.map(&:as_json)
    }
  end

end
