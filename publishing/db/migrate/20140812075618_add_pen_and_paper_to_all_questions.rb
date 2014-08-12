class AddPenAndPaperToAllQuestions < ActiveRecord::Migration
  # This migration is destructive
  def up
    Question.all.each do |question|
      question.update_attribute(:tools, Tools.default)
    end
  end

  def down
  end
end
