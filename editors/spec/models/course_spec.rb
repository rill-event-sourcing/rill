require 'rails_helper'

RSpec.describe Course, :type => :model do

  it {is_expected.to validate_presence_of :name }

  it "should return name when asked for its string" do
    @course = build(:course)
    expect(@course.to_s).to eq @course.name
  end

  it {is_expected.to have_many :chapters}

end
