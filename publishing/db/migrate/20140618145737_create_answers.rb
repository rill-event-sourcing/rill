class CreateAnswers < ActiveRecord::Migration
  def change
    create_table :answers, id: :uuid do |t|
      t.uuid :line_input_id, index: true
      t.string :value
      t.boolean :correct
      t.timestamps
    end
  end
end
