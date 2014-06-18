class CreateCourses < ActiveRecord::Migration
  def change
    create_table :courses, id: :uuid do |t|
      t.string :name
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.timestamps
    end
  end
end
