class Question < ActiveRecord::Base
  include Trashable, Activateable

  validates :text, presence: true

  belongs_to :section, touch: true

end
