class CreateChapters < ActiveRecord::Migration
  def change
    create_table :chapters do |t|
      t.references :course, index: true
      t.string :title
      t.text :description
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.integer :order, limit: 3
      t.timestamps
    end
  end
end
