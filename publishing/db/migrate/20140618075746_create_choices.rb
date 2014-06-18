class CreateChoices < ActiveRecord::Migration
  def change
    create_table :choices, id: :uuid do |t|
      t.uuid :multiple_choice_question_id, index: true
      t.text :value
      t.boolean :correct
      t.timestamps
    end
  end
end
