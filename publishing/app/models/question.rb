class Question < ActiveRecord::Base
  include Trashable, Activateable
end
