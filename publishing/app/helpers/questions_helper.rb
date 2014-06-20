module QuestionsHelper

  def question_to_html(question)
    html = content_tag(:div, question.text)
    question.inputs.each do |input|
      input_html = input_to_html(input)
      html.gsub!(input.name, input_html)
    end
    html.gsub!(/(_INPUT_[0-9]+_)/, content_tag(:div, 'Remove \1 from the source!', class: "alert alert-danger"))
    html.html_safe
  end

  def input_to_html(input)
    if input.line_input?
      line_input_to_html(input)
    elsif input.multiple_choice_input?
      multiple_choice_input_to_html(input)
    else
      content_tag(:div, 'unknown input type')
    end
  end

  def line_input_to_html(input)
    content_tag(:div, class: "input-group", style: "width: 300px;") do
      html = ""
      html << content_tag(:span, input.pre, class: "input-group-addon") unless input.pre.blank?
      html << content_tag(:input, nil, class: 'form-control')
      html << content_tag(:span, input.post, class: "input-group-addon") unless input.post.blank?
      html.html_safe
    end
  end

  def multiple_choice_input_to_html(input)
    content_tag(:select, class: "form-control", style: "width: 300px;", size: input.choices.count) do
      input.choices.map{|ch| content_tag(:option, ch.value) }.join('').html_safe
    end
  end

end
