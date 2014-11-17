class CreateReflections < ActiveRecord::Migration
  def change
    create_table :reflections, id: :uuid do |t|
      t.uuid :section_id, index: true
      t.integer :position, limit: 2
      t.text :content
      t.text :answer
      t.timestamps
    end
    add_column :sections, :reflection_counter, :integer, limit: 2
  end
end
