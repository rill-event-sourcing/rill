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
      learning_tree[chapter] = {}
      ActiveRecord::Base.connection.select_all(
        "SELECT id, chapter_id, name, description, active, position FROM topics"+
        " WHERE CHAR_LENGTH(name) > 0"+
        " AND chapter_id=#{ chapter['id'] }"
      ).each do |topic|
        contents = ActiveRecord::Base.connection.select_all(
          "SELECT id, body FROM contents"+
          " WHERE contentable_type='Topic'"+
          " AND CHAR_LENGTH(body) > 0"+
          " AND KIND IN (2,3)"+
          " AND contentable_id=#{ topic['id'] }"
        )
        learning_tree[chapter][topic] = contents
      end
    end

# contents
  #  id               | integer                     | not null default nextval('contents_id_seq'::regclass)
  #  body             | text                        |
  #  contentable_id   | integer                     |
  #  contentable_type | character varying(255)      |
  #  kind             | integer                     |
  #  created_at       | timestamp without time zone | not null
  #  updated_at       | timestamp without time zone | not null
# description_templates
  #  id               | integer                     | not null default nextval('description_templates_id_seq'::regclass)
  #  body             | text                        |
  #  part_template_id | integer                     |
  #  created_at       | timestamp without time zone | not null
  #  updated_at       | timestamp without time zone | not null

    ###################################################################
    ActiveRecord::Base.establish_connection :development
    course_id = Course.first.id

    learning_tree.each do |chapter, topic_hash|
      new_chapter = Chapter.create!(
        course_id: course_id,
        title: chapter['name'],
        description: chapter['description'],
        active: chapter['active'],
        position: chapter['position']
      )
      topic_hash.each do |topic, contents|
        new_topic = Section.create!(
          chapter: new_chapter,
          title: topic['name'],
          description: topic['description'],
          active: topic['active'],
          position: topic['position']
        )
        # contents.each do |content|
        #   Subsection.create!(
        #     section: new_topic,
        #     title: content['body'][0,20],
        #     description: content['body'],
        #     level: 2
        #   )
        # end
      end
    end

    ###################################################################
  end

end
