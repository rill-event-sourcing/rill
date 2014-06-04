namespace :importer do

  desc 'Import all learning material'
  task :import => :environment do
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE courses")
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE chapters")
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE sections")

    math = Course.create(name: 'Math')

    ActiveRecord::Base.establish_connection :prev_development
    chapters = ActiveRecord::Base.connection.select_all(
      "SELECT id, name, description, active, position from chapters"
    )
    topics = ActiveRecord::Base.connection.select_all(
      "SELECT id, chapter_id, name, description, active, position from topics"
    )

    ActiveRecord::Base.establish_connection :development
    course_id = Course.first.id

    chapters.each do |result|
      Chapter.create!(
        id: result['id'],
        course_id: course_id,
        title: result['name'],
        description: result['description'],
        active: result['active'],
        position: result['position']
      )
    end

    topics.each do |result|
      Section.create(
        id: result['id'],
        chapter_id: result['chapter_id'],
        title: result['name'],
        description: result['description'],
        active: result['active'],
        position: result['position']
      )
    end


  end

end
