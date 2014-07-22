class CreateQuestions < ActiveRecord::Migration
  def change
    create_table :questions, id: :uuid do |t|
      t.uuid :quizzable_id, index: true
      t.string :quizzable_type
      t.text :text
      t.text :worked_out_answer
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.integer :max_inputs, limit: 2
      t.timestamps
    end
  end
end
