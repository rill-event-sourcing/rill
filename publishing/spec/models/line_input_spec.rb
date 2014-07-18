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

end
