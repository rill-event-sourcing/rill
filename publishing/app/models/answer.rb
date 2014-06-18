class Answer < ActiveRecord::Base

  belongs_to :open_question

  validates :value, :presence => true

end
