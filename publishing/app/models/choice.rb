class Choice < ActiveRecord::Base

  belongs_to :multiple_choice_question

  validates :value, presence: true

end
