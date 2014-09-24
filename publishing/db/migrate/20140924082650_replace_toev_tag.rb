class ReplaceToevTag < ActiveRecord::Migration
  #This is a destructive migration!

  class Question < ActiveRecord::Base
  end

  class Subsection < ActiveRecord::Base
  end

  def strip_toev_tag(text)
    text.gsub(/<toev>/, "<p class=\"question-comment\">").gsub(/<\/toev>/, "</p>")
  end

  def up
    Question.all.each do |q|
      q.update_attribute(:text, strip_toev_tag(q.text))
    end

    Subsection.all.each do |s|
      s.update_attribute(:text, strip_toev_tag(s.text))
    end
  end

  def down
  end
end
