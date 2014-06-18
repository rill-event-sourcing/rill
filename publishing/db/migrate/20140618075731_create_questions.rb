class CreateQuestions < ActiveRecord::Migration
  def change
    create_table :questions, id: :uuid do |t|
      t.uuid :question_id, index: true
      t.text :text
      t.datetime :deleted_at
      t.boolean :active, default: false
      t.timestamps
    end
    add_index :questions, :deleted_at
  end
end
