class AddPenAndPaperToAllQuestions < ActiveRecord::Migration
  # This migration is destructive
  class Question < ActiveRecord::Base
  end

  def up
    Question.update_all("tools = '{\"pen_and_paper\" => 1}'")
  end

  def down
  end
end
