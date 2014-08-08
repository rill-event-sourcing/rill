require 'rails_helper'

RSpec.describe MultipleChoiceInput, type: :model do

  it {is_expected.to have_many :choices}

  before do
    @mc = create(:multiple_choice_input)
    @mc_with_choice = create(:multiple_choice_input)
    @choice = create(:choice, value: "something", multiple_choice_input: @mc_with_choice, correct: true)
    @mc_with_empty_choice = create(:multiple_choice_input)
    @empty_choice = create(:choice, value: "", multiple_choice_input: @mc_with_empty_choice, correct: false)
  end

  it "should return an abbreviated uuid" do
    id = @mc.id.to_s
    expect(@mc.to_param).to eq id[0,8]
  end

  it "should make sure there are choices to select" do
    expect(@mc.errors_when_publishing).to include("No choice for #{@mc.name} in question '#{@mc.question.name}' in '#{@mc.question.quizzable}'")
    expect(@mc_with_choice.errors_when_publishing).not_to include("No choice for #{@mc_with_choice.name} in question #{@mc_with_choice.question.name} in '#{@mc_with_choice.question.quizzable}'")
  end

  it "should make sure at least one choice is marked as correct" do
    expect(@mc.errors_when_publishing).to include("No correct choice for #{@mc.name} in question '#{@mc.question.name}' in '#{@mc.question.quizzable}'")
    expect(@mc_with_choice.errors_when_publishing).not_to include("No correct choice for #{@mc_with_choice.name} in question #{@mc_with_choice.question.name} in '#{@mc_with_choice.question.quizzable}'")
  end

  it "should make sure every choice is non empty" do
    expect(@mc_with_choice.errors_when_publishing).not_to include("Empty choice for #{@mc_with_choice.name} in question '#{@mc_with_choice.question.name}' in '#{@mc_with_choice.question.quizzable}'")
    expect(@mc_with_empty_choice.errors_when_publishing).to include("Empty choice for #{@mc_with_empty_choice.name} in question '#{@mc_with_empty_choice.question.name}' in '#{@mc_with_empty_choice.question.quizzable}'")
  end

end
