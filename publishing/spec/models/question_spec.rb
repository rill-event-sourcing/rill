require 'rails_helper'

RSpec.describe Question, :type => :model do

  before do
    @question = create(:question)
    @question2 = create(:question)
    @question3 = create(:question)
  end

  it "should always have html" do
    @question = build(:question)
    @question.text = nil
    @question.save
    expect(@question.text).to eq ""
  end

  it "should default have 'pen & paper' as tools" do
    @question = create(:question)
    expect(@question.tools).to eq Tools.default
  end

  it "should return an abbreviated uuid" do
    id = @question.id.to_s
    expect(@question.to_param).to eq id[0,8]
  end

  it "should return its text when asked for its string" do
    expect(@question.to_s).to eq @question.text
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Question.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Question.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Question.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple questions by an abbreviated uuid" do
    uuid = Question.first.id
    Question.all.each do |question|
      question.update_attribute :id, uuid[0,8] + question.id[8,28]
    end
    expect{Question.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should set correctly max position for the first created input" do
    expect(@question.max_inputs).to eq nil
    @input = create(:line_input, question: @question)
    expect(@question.max_inputs).to eq 1
  end

  it "should increase max position when new inputs are generated" do
    @input = create(:line_input, question: @question)
    max_inputs = @question.max_inputs
    @input2 = create(:line_input, question: @question)
    expect(@question.max_inputs).to eq (max_inputs+1)
  end

  it "should detect when inputs are referenced exactly once" do
   expect(@question.errors_when_publishing).to include("No Inputs on question '#{@question.name}', in '#{@question.quizzable}'")
   create(:line_input, question: @question)
   expect(@question.errors_when_publishing).not_to include("No Inputs on question '#{@question.name}', in '#{@question.quizzable}'")
  end

  it "should make sure all inputs are referenced" do
    @input = create(:line_input, question: @question)
    expect(@question.errors_when_publishing).to include("Error in input referencing in question '#{@question.name}', in '#{@question.quizzable}'")
    @question.text = "#{@input.name}"
    expect(@question.errors_when_publishing).not_to include("Error in input referencing in question '#{@question.name}', in '#{@question.quizzable}'")
  end

  it "should make sure nonexisisting inputs are not referenced" do
    @input = create(:line_input, question: @question)
    @question.text = "_INPUT_#{@input.position+1}_"
    expect(@question.errors_when_publishing).to include("Nonexisting inputs referenced in question '#{@question.name}', in '#{@question.quizzable}'")
    @question.text = "_INPUT_#{@input.position}_"
    expect(@question.errors_when_publishing).not_to include("Nonexisting inputs referenced in question '#{@question.name}', in '#{@question.quizzable}'")
  end

end
