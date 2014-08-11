require 'rails_helper'

RSpec.describe QuestionsHelper, :type => :helper do

  describe "question_worked_out_answer_to_html" do
    it "should return html with the worked out answer of the question" do
      question = build(:question)
      expect(helper.question_worked_out_answer_to_html(question)).to match /#{question.worked_out_answer}/
    end
  end


end
