class Question < ActiveRecord::Base
  include Trashable, Activateable

  validates :text, presence: true
  validates :section, presence: true

  belongs_to :section, touch: true

  def to_s
    text
  end

  def to_param
    "#{id[0,8]}"
  end

end
