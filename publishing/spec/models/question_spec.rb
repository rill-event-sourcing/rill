require 'rails_helper'

RSpec.describe Question, :type => :model do

  before do
    @entry_quiz = create(:entry_quiz)
    @question = create(:question, quizzable: @entry_quiz)
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

  describe "should create a worked_out_answer when none is provided" do

    it "with a single line input" do
      question = create(:question, worked_out_answer: "")
      input1 = create(:line_input, inputable: question)
      answer1 = create(:answer, line_input: input1, value: 'correct')
      published_format = question.to_publishing_format_for_section
      expect(published_format[:worked_out_answer]).to eq "<div class=\"m-answer\">Het juiste antwoord is: correct</div>"
    end

    it "with a single multiple choice input" do
      question = create(:question, worked_out_answer: "")
      input1 = create(:multiple_choice_input, inputable: question)
      choice1 = create(:choice, value: "correct", multiple_choice_input: input1, correct: true)
      choice1 = create(:choice, value: "not correct", multiple_choice_input: input1, correct: false)
      published_format = question.to_publishing_format_for_section
      expect(published_format[:worked_out_answer]).to eq "<div class=\"m-answer\">Het juiste antwoord is: correct</div>"
    end

  end

  it "should not export a worked out answer when exporting for entry quiz" do
    question = create(:question, worked_out_answer: "")
    input1 = create(:line_input, inputable: question)
    answer1 = create(:answer, line_input: input1, value: 'goed')
    published_format = question.to_publishing_format_for_entry_quiz
    expect(published_format.keys).not_to include "worked_out_answer"
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
    @input = create(:line_input, inputable: @question)
    expect(@question.max_inputs).to eq 1
  end

  it "should increase max position when new inputs are generated" do
    @input = create(:line_input, inputable: @question)
    max_inputs = @question.max_inputs
    @input2 = create(:line_input, inputable: @question)
    expect(@question.max_inputs).to eq (max_inputs+1)
  end

  describe "enforcing constraints for publishing" do

    it "should make sure there is at least one input" do
      expect(@question.errors_when_publishing).to include("No Inputs on #{@question.reference}")
      create(:line_input, inputable: @question)
      expect(@question.errors_when_publishing).not_to include("No Inputs on #{@question.reference}")
    end

    it "should make sure all inputs are referenced" do
      @input = create(:line_input, inputable: @question)
      expect(@question.errors_when_publishing).to include("Error in input referencing in #{@question.reference}")
      @question.text = "#{@input.name}"
      expect(@question.errors_when_publishing).not_to include("Error in input referencing in #{@question.reference}")
    end

    it "should make sure nonexisisting inputs are not referenced" do
      @input = create(:line_input, inputable: @question)
      @question.text = "_INPUT_#{@input.position+1}_"
      expect(@question.errors_when_publishing).to include("Nonexisting inputs referenced in #{@question.reference}")
      @question.text = "_INPUT_#{@input.position}_"
      expect(@question.errors_when_publishing).not_to include("Nonexisting inputs referenced in #{@question.reference}")
    end

    it "should make sure worked out answers are not enforced for an entry quiz" do
      @input1 = create(:line_input, inputable: @question)
      create(:answer, line_input: @input1, value: 'good')
      @question.text = "_INPUT_#{@input1.position}_"
      expect(@question.errors_when_publishing_for_entry_quiz).not_to include("No Worked-out-answer given for #{@question.reference}")
    end

    it "should make sure that whenever there are multiple inputs, there is a worked out answer" do
      @input1 = create(:line_input, inputable: @question)
      create(:answer, line_input: @input1, value: 'good')
      @question.text = "_INPUT_#{@input1.position}_"
      expect(@question.errors_when_publishing).not_to include("No Worked-out-answer given for #{@question.reference}")

      @input2 = create(:line_input, inputable: @question)
      create(:answer, line_input: @input2, value: 'better')
      @question.text = "_INPUT_#{@input1.position}_ _INPUT_#{@input2.position}_"
      @question.worked_out_answer = nil
      expect(@question.errors_when_publishing).to include("No WOA given for #{@question.reference}")

      @question.worked_out_answer = "Just do it!"
      expect(@question.errors_when_publishing).not_to include("No WOA given for #{@question.reference}")
    end

  end

  it "should give image errors when publishing" do
    @input1 = create(:line_input, inputable: @question)
    create(:answer, line_input: @input1, value: 'good')

    @question.text = "_INPUT_#{@input1.position}_"
    expect(@question.errors_when_publishing).to eq []

    @question.text = %(_INPUT_#{@input1.position}_<img src="https://www.example.org/test.jpg">)
    expect(@question.errors_when_publishing.count).to eq 1
    expect(@question.errors_when_publishing.first).to match %(`https://www.example.org/test.jpg` is not a valid image source)
  end

  it "should give image errors when publishing" do
    @input1 = create(:line_input, inputable: @question)
    create(:answer, line_input: @input1, value: 'good')
    @question.text = "_INPUT_#{@input1.position}_"

    @question.worked_out_answer = "OK"
    expect(@question.errors_when_publishing).to eq []

    @question.worked_out_answer = %(OK <img src="https://www.example.org/test.jpg">)
    expect(@question.errors_when_publishing.count).to eq 1
    expect(@question.errors_when_publishing.first).to match %(`https://www.example.org/test.jpg` is not a valid image source)
  end

  it "should give parse errors when publishing" do
    @input1 = create(:line_input, inputable: @question)
    create(:answer, line_input: @input1, value: 'good')

    @question.text = "<p>_INPUT_#{@input1.position}_</p>"
    expect(@question.errors_when_publishing).to eq []

    @question.text = "<p>_INPUT_#{@input1.position}_"
    expect(@question.errors_when_publishing.count).to eq 1
    expect(@question.errors_when_publishing.first).to match "parse error"
  end

  it "should give parse errors when publishing" do
    @input1 = create(:line_input, inputable: @question)
    create(:answer, line_input: @input1, value: 'good')
    @question.text = "<p>_INPUT_#{@input1.position}_</p>"

    @question.worked_out_answer = "<p>OK</p>"
    expect(@question.errors_when_publishing).to eq []

    @question.worked_out_answer = "<p>OK"
    expect(@question.errors_when_publishing.count).to eq 1
    expect(@question.errors_when_publishing.first).to match "parse error"
  end

end
