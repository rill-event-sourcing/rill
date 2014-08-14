class CreateEntryQuizzes < ActiveRecord::Migration
  def change
    create_table :entry_quizzes, id: :uuid do |t|
      t.uuid :course_id, index: true
      t.text :instructions
      t.text :feedback
      t.boolean :active, default: false
      t.integer :threshold, limit: 2, default: 0
      t.timestamps
    end
  end
end
