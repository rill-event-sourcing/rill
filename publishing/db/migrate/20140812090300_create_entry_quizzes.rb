class CreateEntryQuizzes < ActiveRecord::Migration
  def change
    create_table :entry_quizzes, id: :uuid do |t|
      t.uuid :course_id, index: true
      t.text :instructions
      t.boolean :active, default: false
      t.timestamps
    end
  end
end
