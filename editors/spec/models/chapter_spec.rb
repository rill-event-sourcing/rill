require 'rails_helper'

RSpec.describe Chapter, :type => :model do
  it {is_expected.to validate_presence_of :title }
  #it {is_expected.to validate_presence_of :course }

  it "should return title when asked for its string" do
    @chapter = build(:chapter)
    expect(@chapter.to_s).to eq @chapter.title
  end
end
