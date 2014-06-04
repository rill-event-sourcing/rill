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

ActiveRecord::Schema.define(version: 20140604083402) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "chapters", force: true do |t|
    t.integer  "course_id"
    t.string   "title"
    t.text     "description"
    t.datetime "deleted_at"
    t.boolean  "active",      default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "chapters", ["course_id"], name: "index_chapters_on_course_id", using: :btree

  create_table "courses", force: true do |t|
    t.string   "name"
    t.datetime "deleted_at"
    t.boolean  "active",     default: false
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "sections", force: true do |t|
    t.integer  "chapter_id"
    t.string   "title"
    t.text     "description"
    t.datetime "deleted_at"
    t.boolean  "active",      default: false
    t.integer  "position"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  add_index "sections", ["chapter_id"], name: "index_sections_on_chapter_id", using: :btree

end
