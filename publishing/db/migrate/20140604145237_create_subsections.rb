class CreateSubsections < ActiveRecord::Migration
  def change
    create_table :subsections, id: :uuid  do |t|
      t.uuid :section_id, index: true
      t.string :title
      t.text :text
      t.integer :stars, limit: 1
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.integer :position, limit: 3
      t.timestamps
    end
    add_index :subsections, :deleted_at
  end
end
