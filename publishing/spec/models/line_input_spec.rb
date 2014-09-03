require 'rails_helper'

RSpec.describe LineInput, type: :model do

  it {is_expected.to have_many :answers}

  before do
    @question = create(:question)
    @line_input = create(:line_input, inputable: @question)
    @line_input_with_answer = create(:line_input, inputable: @question)
    @answer = create(:answer, value: "something", line_input: @line_input_with_answer)
  end

  it "should return an abbreviated uuid" do
    id = @line_input.id.to_s
    expect(@line_input.to_param).to eq id[0,8]
  end

  describe "enforcing constraints for publishing" do

    it "should make sure there is at least one correct answer" do
      expect(@line_input.errors_when_publishing).to include( "No correct answer for line input #{@line_input.name} in #{@line_input.inputable_type} '#{@line_input.inputable.name}', in '#{@line_input.inputable.parent}'")
      expect(@line_input_with_answer.errors_when_publishing).not_to include( "No correct answer for line input #{@line_input_with_answer.name} in #{@line_input.inputable_type} '#{@line_input.inputable.name}', in '#{@line_input.inputable.parent}'")
    end

    it "should make sure correct answers are nonempty" do
      expect(@line_input_with_answer.errors_when_publishing).not_to include("Empty correct answer for line input #{@line_input_with_answer.name} in #{@line_input.inputable_type} '#{@line_input_with_answer.inputable.name}', in '#{@line_input_with_answer.inputable.parent}'")

      line_input_with_empty_answer = create(:line_input, inputable: @question)
      empty_answer = create(:answer, value: "", line_input: line_input_with_empty_answer)

      expect(line_input_with_empty_answer.errors_when_publishing).to include("Empty correct answer for line input #{line_input_with_empty_answer.name} in #{line_input_with_empty_answer.inputable_type} '#{line_input_with_empty_answer.inputable.name}', in '#{line_input_with_empty_answer.inputable.parent}'")
    end

  end

end
