namespace :html do
  desc "fix html br"
  task :br => :environment  do
    connection = ActiveRecord::Base.connection
    {
      "</br>" => "<br>"
    }.each do |old, new|
      connection.execute "UPDATE questions   SET text  = replace(text,  '#{old}', '#{new}')"
      connection.execute "UPDATE subsections SET text  = replace(text,  '#{old}', '#{new}')"
      connection.execute "UPDATE choices     SET value = replace(value, '#{old}', '#{new}')"
      connection.execute "UPDATE questions   SET worked_out_answer = replace(worked_out_answer, '#{old}', '#{new}')"
    end
  end

  desc "fix html question-comment and question-title classes"
  task :classes => :environment  do
    connection = ActiveRecord::Base.connection
    {
      "question_comment" => "question-comment",
      "question_title"   => "question-title"
    }.each do |old, new|
      connection.execute "UPDATE questions   SET text  = replace(text,  '#{old}', '#{new}')"
      connection.execute "UPDATE subsections SET text  = replace(text,  '#{old}', '#{new}')"
      connection.execute "UPDATE choices     SET value = replace(value, '#{old}', '#{new}')"
      connection.execute "UPDATE questions   SET worked_out_answer = replace(worked_out_answer, '#{old}', '#{new}')"
    end
  end

  desc "fix html question classes"
  task :question => :environment  do
    [Question, Subsection].each do |html_type|
      p html_type.to_s
      html_type.where([
                       "text LIKE ? OR text LIKE ?",
                       '%<div class="question">%',
                       '%<div class="m-question">%'
                     ]).each do |item|
        p item.id
        text = item.text.chomp
        p text
        text.gsub!(/<div class="question">(.*?)<\/div>/im, "\\1")
        text.gsub!(/<div class="m-question">(.*?)<\/div>/im, "\\1")
        text.gsub!(%(<div class="question">), "")
        text.gsub!(%(<div class="m-question">), "")
        text = text.strip.chomp.strip.chomp
        p text
        item.update_attribute(:text, text)
      end
    end
  end

end
