namespace :exporter do
  desc "Export learning material to json"
  task :export => :environment  do
    require 'json'
    name = ENV["COURSE_NAME"]
    unless name
      warn "Usage: COURSE_NAME=yourname rake exporter:export"
      exit 1
    end
    print JSON.pretty_generate(Course.where( name: name).first.as_json)
  end
end
