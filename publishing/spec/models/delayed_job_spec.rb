require 'rails_helper'

RSpec.describe DelayedJob, :type => :model do

  it "should have correct statuses" do
    @delayed_job = build(:delayed_job)
    expect(@delayed_job.status).to eq :scheduled

    @delayed_job = build(:delayed_job, failed_at: Time.now)
    expect(@delayed_job.status).to eq :failed

    @delayed_job = build(:delayed_job, locked_at: Time.now)
    expect(@delayed_job.status).to eq :running
  end

end
