class CreateExtraExamples < ActiveRecord::Migration
  def change
    create_table :extra_examples, id: :uuid do |t|
      t.uuid :section_id, index: true
      t.integer :position, limit: 2
      t.string :title
      t.text :content
      t.timestamps
    end
    add_column :sections, :extra_example_counter, :integer, limit: 2
  end
end
