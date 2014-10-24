class CreateChapterQuestionsSets < ActiveRecord::Migration
  def change
    create_table :chapter_questions_sets, id: :uuid do |t|
      t.uuid :chapter_quiz_id
      t.integer :position, limit: 3
      t.string :title
      t.timestamps
    end
  end
end
