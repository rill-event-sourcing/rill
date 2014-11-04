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

  desc "fix html classes"
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

end
