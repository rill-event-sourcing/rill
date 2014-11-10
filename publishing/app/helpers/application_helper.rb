module ApplicationHelper

  def tools(tools_hash)
    html = ""
    tools_hash.each do |k,v|
      next unless v.to_i > 0
      html << content_tag(:div, "", class: "tools #{k}")
    end
    html.html_safe
  end

  def text_to_html(inputs, text)
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
    htmlClass = 'small-input'
    htmlClass = 'big-input'      if input.style == 'big'
    htmlClass = 'exponent-input' if input.style == 'exponent'
    htmlClass += ' block-input'  if input.inline?

    htmlLength = '5'
    htmlLength = '14' if input.style == 'big'
    htmlLength = '2'  if input.style == 'exponent'

    content_tag(:span) do
      content_tag(:span, input.prefix) +
      content_tag(:input, nil, class: htmlClass, maxlength: htmlLength) +
      content_tag(:span, input.suffix)
    end
  end

  def multiple_choice_input_to_html(input)
    content_tag(:span, class: "mc-list") do
      input.choices.map do |ch|
        content_tag(:span, class: "mc-choice") do
          content_tag(:label) do
            radio_button_tag("#{ch.multiple_choice_input.id}", nil, ch.correct?) +
            content_tag(:span, render_latex_for_editing(ch.value).html_safe)
          end
        end
      end.join('').html_safe
    end
  end


end
