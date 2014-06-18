class CreateChapters < ActiveRecord::Migration
  def change
    create_table :chapters, id: :uuid do |t|
      t.uuid :course_id, index: true
      t.string :title
      t.text :description
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.integer :position, limit: 3
      t.timestamps
    end
  end
end
