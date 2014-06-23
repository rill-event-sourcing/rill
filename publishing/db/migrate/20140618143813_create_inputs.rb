class CreateInputs < ActiveRecord::Migration
  def change
    create_table :inputs, id: :uuid do |t|
      t.uuid :question_id, index: true
      t.string :type
      t.integer :position, limit: 2
      t.string :pre
      t.string :post
      t.integer :width, limit: 2
      t.timestamps
    end
  end
end
