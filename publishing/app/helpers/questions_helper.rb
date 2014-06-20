module QuestionsHelper

  def question_to_html(question)
    html = content_tag(:div, question.text)
    question.inputs.each do |input|
      html << input_to_html(input)
    end
    html
  end

  def input_to_html(input)
    if input.line_input?
      content_tag(:input)
    elsif input.multiple_choice_input?
      content_tag(:ul) do
        input.choices.map{|ch| content_tag(:li, ch.value) }.join('').html_safe
      end
    else
      content_tag(:div, 'unknown input type')
    end
  end

end
