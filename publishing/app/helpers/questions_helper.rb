module QuestionsHelper

  def question_worked_out_answer_to_html(question)
    question.worked_out_answer = render_latex_for_editing(question.worked_out_answer).html_safe
  end

end
