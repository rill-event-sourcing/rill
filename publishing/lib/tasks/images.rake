namespace :images do

  desc "index images to database"
  task :index => :environment do
    index_images()
  end

  def index_images(dir = "")
    path = "#{ Image.bucket_dir }/#{ dir }"
    Dir.open(path).each do |file|
      next if file[0, 1] == '.'
      if File.directory?("#{ path }/#{ file }")
        index_images("#{ dir }/#{ file }")
      else
        Image.create(path: "#{ dir }/#{ file }")
      end
    end
  end

  desc "check asset images dimenstions"
  task :check => :environment do
    ##############################
    recheck = 30.minutes
    limit   = 1000
    ##############################
    outdated = Image.outdated(DateTime.now - recheck)
    images = outdated.limit(limit)
    p "#{ images.count } / #{ outdated.count } images"
    images.map(&:check)
  end

  desc "check all asset images dimenstions"
  task :check_all => :environment do
    images = Image.all
    p "#{ images.count } images"
    images.map(&:check)
  end

end
