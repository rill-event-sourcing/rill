require 'rails_helper'

RSpec.describe ApplicationHelper, :type => :helper do


  describe "question_text_to_html" do
    it "should return html with the text of the question" do
      question = build(:question)
      expect(helper.text_to_html(question.inputs,question.text)).to match /question.text/
    end

    it "should return html with the warning when input is not found" do
      question = build(:question)
      question.text << " _INPUT_99_"
      expect(helper.text_to_html(question.inputs, question.text)).to match /alert-danger/
    end

    it "should call input_to_html for each input" do
      question = build(:question)
      question.inputs << build(:line_input, position: 1)
      question.inputs << build(:multiple_choice_input, position: 2)
      expect(helper).to receive(:input_to_html).twice.with(any_args()).and_return('')
      helper.text_to_html(question.inputs,question.text)
    end
  end


  describe "input_to_html" do
    it "should return html for the line-input type" do
      input = build(:line_input)
      expect(helper).to receive(:line_input_to_html).once.with(input)
      helper.input_to_html(input)
    end

    it "should return html for the multiple-choice-input type" do
      input = build(:multiple_choice_input)
      expect(helper).to receive(:multiple_choice_input_to_html).once.with(input)
      helper.input_to_html(input)
    end

    it "should return html with unknown for an unknown input type" do
      class FakeInput < Input; end
      input = FakeInput.new
      expect(helper.input_to_html(input)).to match /unknown input type/
    end
  end


  describe "line_input_to_html" do
    it "should return an input field for the input" do
      input = build(:line_input)
      expect(helper.line_input_to_html(input)).to match /input/
    end

    it "should return a prefix addon field for the input" do
      input = build(:line_input, prefix: 'prefix-text')
      expect(helper.line_input_to_html(input)).to match /prefix-text/
    end

    it "should return a suffix addon field for the input" do
      input = build(:line_input, suffix: 'suffix-text')
      expect(helper.line_input_to_html(input)).to match /suffix-text/
    end
  end


  describe "multiple_choice_input_to_html" do
    it "should return an multiple choice input field for the input" do
      input = build(:multiple_choice_input)
      input.choices << build(:choice, value: 'choice A')
      input.choices << build(:choice, value: 'choice B')
      expect(helper.multiple_choice_input_to_html(input)).to match /div/
      expect(helper.multiple_choice_input_to_html(input)).to match /button/
      expect(helper.multiple_choice_input_to_html(input)).to match /choice A/
      expect(helper.multiple_choice_input_to_html(input)).to match /choice B/
    end
  end


end
