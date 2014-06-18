require 'rails_helper'

RSpec.describe LineInput, type: :model do

  it {is_expected.to have_many :answers}

  before do
    @open_question = create(:line_input)
  end

it "should return the text when asked for a string" do
  expect(@open_question.to_s).to eq @open_question.text
end

it "should return an abbreviated uuid" do
  id = @open_question.id.to_s
  expect(@open_question.to_param).to eq id[0,8]
end

end
