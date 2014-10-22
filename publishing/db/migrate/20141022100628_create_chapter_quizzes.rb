class CreateChapterQuizzes < ActiveRecord::Migration
  def change
    create_table :chapter_quizzes, id: :uuid do |t|
      t.uuid :chapter_id, index: true
      t.timestamps
    end
  end
end
