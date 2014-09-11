class DelayedJob < ActiveRecord::Base

  def status
    if self.failed_at
      :failed
    elsif self.locked_at
      :running
    else
      :scheduled
    end
  end
end
