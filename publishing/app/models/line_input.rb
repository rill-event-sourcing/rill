class LineInput < Input

  has_many :answers

  def as_json
    {
      name: "_INPUT_#{position}_",
      prefix: prefix,
      suffix: suffix,
      width: width,
      correct_answers: answers.map(&:value)
    }
  end

end
