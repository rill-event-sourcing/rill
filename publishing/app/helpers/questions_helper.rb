module QuestionsHelper

  def question_worked_out_answer_to_html(question)
    render_latex_for_editing(question.worked_out_answer).html_safe
  end

end
