module Activateable
  extend ActiveSupport::Concern

  included do
  end

  module ClassMethods
  end

  def activate
    update_attribute :active, true
  end

  def deactivate
    update_attribute :active, false
  end

end
