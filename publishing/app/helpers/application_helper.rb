module ApplicationHelper

  def tools(tools_hash)
    html = ""
    tools_hash.each do |k,v|
      next unless v.to_i > 0
      html << content_tag(:div, "", class: "tools #{k}")
    end
    html.html_safe
  end

  def text_to_html(inputs, text, reflections = [], extra_examples = [])
    text = render_latex_for_editing(text)
    html = text.html_safe

    inputs.each do |input|
      input_html = input_to_html(input)
      html.gsub!(input.name, input_html) if html
    end

    reflections.each do |reflection|
      reflection_html = reflection_to_html(reflection)
      html.gsub!(reflection.name, reflection_html) if html
    end

    extra_examples.each do |extra_example|
      extra_example_html = extra_example_to_html(extra_example)
      html.gsub!(extra_example.name, extra_example_html) if html
    end

    html.gsub!(/(_INPUT_[0-9]+_)/, content_tag(:div, 'Please remove \1 from the source!', class: "alert alert-danger"))
    html.gsub!(/(_REFLECTION_[0-9]+_)/, content_tag(:div, 'Please remove \1 from the source!', class: "alert alert-danger"))
    html.gsub!(/(_EXTRA_EXAMPLE_[0-9]+_)/, content_tag(:div, 'Please remove \1 from the source!', class: "alert alert-danger"))
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
    htmlClass = 'big-input'
    htmlClass = 'small-input'    if input.style == 'small'
    htmlClass = 'exponent-input' if input.style == 'exponent'
    htmlClass += ' block-input'  unless input.inline?

    htmlLength = ''
    htmlLength = '5' if input.style == 'small'
    htmlLength = '3' if input.style == 'exponent'

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

  def reflection_to_html(reflection)
    content_tag(:div, class: 'm-reflection') do
      render_latex_for_editing(reflection.content.to_s).html_safe +
      content_tag(:div, class: 'reflection-answer show') do
        render_latex_for_editing(reflection.answer.to_s).html_safe
      end
    end
  end

  def extra_example_to_html(extra_example)
    content_tag(:div, class: 'm-extra-example') do
      content_tag(:div, class: 'extra-example-title') do
        extra_example.title.to_s.html_safe
      end +
      content_tag(:div, class: 'extra-example-content show') do
        render_latex_for_editing(extra_example.content.to_s).html_safe
      end
    end
  end

end
