class MultipleChoiceQuestion < Question

  has_many :choices

  validates :text, :presence => true

  # def as_json
  #   {
  #     id: id,
  #     name: name,
  #     chapters: chapters.active.map(&:as_json)
  #   }
  # end

end
