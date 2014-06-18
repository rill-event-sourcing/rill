class OpenQuestion < Question

  has_many :answers

  validates :text, :presence => true

  # length
  # pre-text
  # post-text

  # def as_json
  #   {
  #     id: id,
  #     name: name,
  #     chapters: chapters.active.map(&:as_json)
  #   }
  # end

end
