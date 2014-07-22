class CreateInputs < ActiveRecord::Migration
  def change
    create_table :inputs, id: :uuid do |t|
      t.uuid :question_id, index: true
      t.string :type
      t.integer :position, limit: 2
      t.string :prefix, default: ""
      t.string :suffix, default: ""
      t.integer :width, default: 150, limit: 3
      t.timestamps
    end
  end
end
