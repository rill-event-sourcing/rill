class LineInput < Input

  has_many :answers

  def as_json
    {
      id: id,
      pre: pre,
      post: post,
      width: width,
      answers: answers.map(&:as_json)
    }
  end

end
