module ApplicationHelper

  def render_latex(text)
    regexp = /<math>(.*?)<\/math>/m
    matches = text.scan(regexp)
    new_text = text
    matches.each do |array_of_matches|
      match = array_of_matches.first
      rendered_formula = HTTParty.post("http://localhost:16000/", body: "#{match}").parsed_response
      new_text.gsub!("<math>#{match}</math>", rendered_formula)
    end
    new_text
  end

end
