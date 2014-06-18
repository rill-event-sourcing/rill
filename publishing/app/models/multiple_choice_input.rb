class MultipleChoiceInput < Input

  has_many :choices

  # def as_json
  #   {
  #     id: id,
  #     name: name,
  #     chapters: chapters.active.map(&:as_json)
  #   }
  # end

end
