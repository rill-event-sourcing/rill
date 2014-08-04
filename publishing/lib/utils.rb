# -*- coding: utf-8 -*-
def pretty_debug(value = '', type = 'debug', debug_start = false)
  start = Time.now
  logger ||= ::Rails.logger
  logger.send(type, "\e[46;1;31m")
  if @debug_date_time
    txt = "===> ∂: #{ (start - @debug_date_time).round(2) } #{ value.inspect } ===="
    logger.send(type, txt)
  else
    txt = "===> #{ start.strftime("%H:%M:%S") }.#{ start.tv_sec } ============================================================="
    logger.send(type, txt)
    logger.send(type, "\e[1;37m")
    logger.send(type, "===> #{ value.inspect }")
    logger.send(type, "\e[1;31m")
    logger.send(type, "===> =================================================================================")
  end
  logger.send(type, "\e[44;0;30m");
  @debug_date_time = start if debug_start
end

def render_latex(text)
  regexp = /<math>(.*?)<\/math>/m
  matches = text.scan(regexp)
  new_text = text
  matches.each do |array_of_matches|
    match = array_of_matches.first
    begin
      response = HTTParty.post("http://localhost:16000/", body: "#{match}")
    rescue Errno::ECONNREFUSED
      error = "Connection to LaTeX renderer refused"
    rescue Net::ReadTimeout
      error = "Connection to LaTeX renderer had a timeout"
    end
    if response
      rendered_formula = response.parsed_response
      rendered_formula = content_tag(:div, "\"#{match}\" is not valid LaTeX", class: "alert alert-danger") if rendered_formula == "MathJax error"
      new_text.gsub!("<math>#{match}</math>", rendered_formula)
    else
      new_text = content_tag(:div, "Error with LaTeX rendering: #{error}", class: "alert alert-danger")
      break
    end
  end
  new_text
end
