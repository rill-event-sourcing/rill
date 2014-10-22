class CreateChapterQuestionsSets < ActiveRecord::Migration
  def change
    create_table :chapter_questions_sets do |t|
      t.uuid :chapter_quiz_id
      t.integer :position, limit: 3
      t.string :name
      t.timestamps
    end
  end
end
