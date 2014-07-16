class LineInput < Input

  has_many :answers

  def as_json
    {
      name: "_INPUT_#{position}_",
      pre: pre,
      post: post,
      width: width,
      correct_answers: answers.map(&:as_json)
    }
  end

end
