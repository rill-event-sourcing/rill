namespace :images do

  desc "add images to database"
  task :add => :environment do
    Question.all.map{|ss| ss.image_errors(:text) }
    Question.all.map{|ss| ss.image_errors(:worked_out_answer) }
    Choice.all.map{|ss| ss.image_errors(:value) }
    Subsection.all.map{|ss| ss.image_errors(:text) }
    ExtraExample.all.map{|ss| ss.image_errors(:content) }
    Reflection.all.map{|ss| ss.image_errors(:content) }
    Reflection.all.map{|ss| ss.image_errors(:answer) }
    p "#{ Image.count } images"
  end

  desc "check asset images dimenstions"
  task :check => :environment do
    ##############################
    recheck = 30.minutes
    limit = 10000
    ##############################
    outdated = Image.outdated(DateTime.now - recheck)
    images = outdated.limit(limit)
    p "#{ images.count } / #{ outdated.count } images"
    images.map(&:check)
  end

end
