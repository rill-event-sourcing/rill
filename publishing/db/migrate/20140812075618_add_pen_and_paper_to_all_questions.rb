class AddPenAndPaperToAllQuestions < ActiveRecord::Migration
  # This migration is destructive
  class Question < ActiveRecord::Base
  end


  def up
    Question.all.each do |question|
      question.update_attribute(:tools, {"pen_and_paper" => 1})
    end
  end

  def down
  end
end
