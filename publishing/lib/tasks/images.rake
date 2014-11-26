namespace :images do

  desc "check asset images dimenstions"
  task :check => :environment  do
    Image.outdated(DateTime.now - 30.minutes).limit(20).map(&:check)
  end

end
