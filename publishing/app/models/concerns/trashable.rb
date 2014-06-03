module Trashable
  extend ActiveSupport::Concern

  included do
    default_scope { where(deleted_at: nil) }
  end

  module ClassMethods
    def trashed
      self.unscoped.where(self.arel_table[:deleted_at].not_eq(nil))
    end
  end

  def trash
    run_callbacks :destroy do
      update_column :deleted_at, Time.now
    end
  end

  def recover
    # update_column not appropriate here as it uses the default scope
    update_attribute :deleted_at, nil
  end
end
