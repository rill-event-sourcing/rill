require 'rails_helper'

RSpec.describe Question, :type => :model do

  it {is_expected.to validate_presence_of :section }

  before do
    @question = build(:question)
  end

  it "should always have html" do
    @question.text = nil
    @question.save
    expect(@question.text).to eq ""
  end

end
