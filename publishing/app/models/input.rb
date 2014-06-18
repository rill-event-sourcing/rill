class Input < ActiveRecord::Base
  belongs_to :question
  validates :question, presence: true

  def to_param
    "#{id[0,8]}"
  end
  
end
