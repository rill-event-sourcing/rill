class Question < ActiveRecord::Base
  include Trashable, Activateable

  validates :text, :presence => true

end
