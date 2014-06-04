namespace :importer do

  desc 'Import all learning material'
  task :import => :environment do
    ###################################################################
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE courses")
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE chapters")
    ActiveRecord::Base.connection.execute("TRUNCATE TABLE sections")
    math = Course.create(name: 'Math')

    ###################################################################
    ActiveRecord::Base.establish_connection :prev_development
    learning_tree = {}
    ActiveRecord::Base.connection.select_all(
      "SELECT id, name, description, active, position FROM chapters"
    ).each do |chapter|
      learning_tree[chapter] = ActiveRecord::Base.connection.select_all(
        "SELECT id, chapter_id, name, description, active, position FROM topics"+
        " WHERE chapter_id=#{ chapter['id'] }"
      )
    end

    ###################################################################
    ActiveRecord::Base.establish_connection :development
    course_id = Course.first.id

    learning_tree.each do |chapter, topics|
      new_chapter = Chapter.create!(
        course_id: course_id,
        title: chapter['name'],
        description: chapter['description'],
        active: chapter['active'],
        position: chapter['position']
      )
      topics.each do |topic|
        Section.create(
          chapter: new_chapter,
          title: topic['name'],
          description: topic['description'],
          active: topic['active'],
          position: topic['position']
        )
      end
    end

    ###################################################################
  end

end
