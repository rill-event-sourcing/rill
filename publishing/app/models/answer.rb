class Answer < ActiveRecord::Base

  belongs_to :line_input

  validates :value, presence: true

end
