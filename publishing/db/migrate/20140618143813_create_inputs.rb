class CreateInputs < ActiveRecord::Migration
  def change
    create_table :inputs, id: :uuid do |t|
      t.uuid :question_id, index: true
      t.string :type
      t.timestamps
    end
  end
end
