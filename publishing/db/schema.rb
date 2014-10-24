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

ActiveRecord::Schema.define(version: 20141022101653) do

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

  create_table "chapter_questions_sets", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "chapter_quiz_id"
    t.integer  "position"
    t.string   "title"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "chapter_quizzes", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "chapter_id"
    t.boolean  "active",     default: false
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
    t.boolean  "remedial",    default: false
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

  create_table "delayed_jobs", force: true do |t|
    t.integer  "priority",   default: 0, null: false
    t.integer  "attempts",   default: 0, null: false
    t.text     "handler",                null: false
    t.text     "last_error"
    t.datetime "run_at"
    t.datetime "locked_at"
    t.datetime "failed_at"
    t.string   "locked_by"
    t.string   "queue"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "delayed_jobs", ["priority", "run_at"], name: "delayed_jobs_priority", using: :btree

  create_table "entry_quizzes", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "course_id"
    t.text     "instructions"
    t.text     "feedback"
    t.boolean  "active",                 default: false
    t.integer  "threshold",    limit: 2, default: 0
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "inputs", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "inputable_id"
    t.string   "type"
    t.integer  "position",       limit: 2
    t.string   "prefix",                   default: ""
    t.string   "suffix",                   default: ""
    t.integer  "width",                    default: 150
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string   "inputable_type"
  end

  create_table "questions", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "quizzable_id"
    t.string   "quizzable_type"
    t.text     "text"
    t.text     "worked_out_answer"
    t.datetime "deleted_at"
    t.boolean  "active",                      default: true
    t.integer  "max_inputs",        limit: 2
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string   "name",              limit: 5
    t.string   "tools"
    t.integer  "position",          limit: 2
  end

  create_table "sections", id: :uuid, default: "uuid_generate_v4()", force: true do |t|
    t.uuid     "chapter_id"
    t.string   "title"
    t.text     "description"
    t.datetime "deleted_at"
    t.boolean  "active",                       default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer  "max_inputs",         limit: 2
    t.string   "meijerink_criteria"
    t.string   "domains"
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
