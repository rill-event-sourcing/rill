class CreateSubsections < ActiveRecord::Migration
  def change
    create_table :subsections, id: :uuid  do |t|
      t.uuid :section_id, index: true
      t.string :title
      t.text :text
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.integer :position, limit: 3
      t.timestamps
    end
  end
end
