class Choice < ActiveRecord::Base

  belongs_to :multiple_choice_input

  validates :value, presence: true

end
