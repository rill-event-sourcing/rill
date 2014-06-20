namespace :exporter do
  desc "Export learning material to json"
  task :export => :environment  do
    require 'json'
    name = ENV["COURSE_NAME"]
    unless name
      warn "Usage: COURSE_NAME=yourname rake exporter:export"
      exit 1
    end
    course = Course.where(name: name).first
    unless course
      warn "Course '#{name}' not found!\nCourse can be any of: #{Course.all.map(&:name).join(', ')}"
      exit 1
    end
    print JSON.pretty_generate(course.as_json)
  end
end
