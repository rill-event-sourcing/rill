class CreateQuestions < ActiveRecord::Migration
  def change
    create_table :questions, id: :uuid do |t|
      t.uuid :section_id, index: true
      t.text :text
      t.text :explanation
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.integer :max_inputs, limit: 2
      t.timestamps
    end
  end
end
