class CreateQuestions < ActiveRecord::Migration
  def change
    create_table :questions, id: :uuid do |t|
      t.uuid :section_id, index: true
      t.string :type
      t.text :text
      t.datetime :deleted_at, index: true
      t.boolean :active, default: false
      t.timestamps
    end
  end
end
