class CreateCourses < ActiveRecord::Migration
  def change
    create_table :courses do |t|
      t.string :name
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.timestamps
    end
  end
end
