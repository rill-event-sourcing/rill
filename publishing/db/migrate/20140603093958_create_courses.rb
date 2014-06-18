class CreateCourses < ActiveRecord::Migration
  def change
    create_table :courses, id: :uuid do |t|
      t.string :name
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.timestamps
    end
    add_index :courses, :deleted_at
  end
end
