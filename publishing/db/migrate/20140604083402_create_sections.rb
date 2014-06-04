class CreateSections < ActiveRecord::Migration
  def change
    create_table :sections, id: :uuid do |t|
      t.uuid :chapter_id, index: true
      t.string :title
      t.text :description
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.integer :position, limit: 3
      t.timestamps
    end
    add_index :sections, :created_at
  end
end
