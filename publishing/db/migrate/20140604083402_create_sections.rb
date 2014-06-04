class CreateSections < ActiveRecord::Migration
  def change
    create_table :sections do |t|
      t.references :chapter, index: true
      t.string :title
      t.text :description
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.integer :position, limit: 3
      t.timestamps
    end
  end
end
