class ChoiceQuestion < Question

  has_many :choices

  # has many choices true / false (answers)
  # limit to one correct answer

  # def as_json
  #   {
  #     id: id,
  #     name: name,
  #     chapters: chapters.active.map(&:as_json)
  #   }
  # end

end
