# encoding: UTF-8
# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20140618145746) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"
  enable_extension "uuid-ossp"

  create_table "answers", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "line_input_id"
    t.string   "value"
    t.boolean  "correct"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "chapters", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "course_id"
    t.string   "title"
    t.text     "description"
    t.datetime "deleted_at"
    t.boolean  "active",      default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "choices", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "multiple_choice_input_id"
    t.text     "value"
    t.boolean  "correct"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "courses", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.string   "name"
    t.datetime "deleted_at"
    t.boolean  "active",     default: false
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "inputs", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "question_id"
    t.string   "type"
    t.integer  "position",    limit: 2
    t.string   "pre"
    t.string   "post"
    t.integer  "width",       limit: 2
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "questions", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "section_id"
    t.text     "text"
    t.text     "explanation"
    t.datetime "deleted_at"
    t.boolean  "active",                default: false
    t.integer  "max_inputs",  limit: 2
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "sections", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "chapter_id"
    t.string   "title"
    t.text     "description"
    t.datetime "deleted_at"
    t.boolean  "active",      default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "subsections", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "section_id"
    t.string   "title"
    t.text     "text"
    t.datetime "deleted_at"
    t.boolean  "active",     default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

end
