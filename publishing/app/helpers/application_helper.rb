module ApplicationHelper

  def tools(tools_hash)
    html = ""
    tools_hash.each do |k,v|
      next unless v.to_i > 0
      html << content_tag(:div, "", class: "tools #{k}")
    end
    html.html_safe
  end

  def text_to_html(inputs,text)
    text = render_latex_for_editing(text)
    html = text.html_safe
    inputs.each do |input|
      input_html = input_to_html(input)
      html.gsub!(input.name, input_html)
    end
    html.gsub!(/(_INPUT_[0-9]+_)/, content_tag(:div, 'Please remove \1 from the source!', class: "alert alert-danger"))
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
    content_tag(:div, class: "input-group", style: "width:#{input.width}px") do
      html = ""
      html << content_tag(:span, input.prefix, class: "input-group-addon") unless input.prefix.blank?
      html << content_tag(:input, nil, class: 'form-control')
      html << content_tag(:span, input.suffix, class: "input-group-addon") unless input.suffix.blank?
      html.html_safe
    end
  end

  def multiple_choice_input_to_html(input)
    content_tag(:div, style: "width: 300px;") do
      input.choices.map{|ch| content_tag(:button, render_latex_for_editing(ch.value).html_safe, class: "btn #{ ch.correct ? 'btn-success' : 'btn-default' } btn-block") }.join('').html_safe
    end
  end


end
