require 'rails_helper'

RSpec.describe LineInput, type: :model do

  it {is_expected.to have_many :answers}

  before do
    @line_input = create(:line_input)
  end

  it "should return an abbreviated uuid" do
    id = @line_input.id.to_s
    expect(@line_input.to_param).to eq id[0,8]
  end

  it "should have the correct format for publishing" do
    obj = {
      name: "_INPUT_#{@line_input.position}_",
      prefix: @line_input.prefix,
      suffix: @line_input.suffix,
      width: @line_input.width,
      correct_answers: @line_input.answers.map(&:value)
    }
    expect(@line_input.as_json).to eq obj
  end

end
