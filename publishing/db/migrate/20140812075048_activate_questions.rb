class ActivateQuestions < ActiveRecord::Migration
  # This migration is destructive
  def up
    Question.all.each do |question|
      question.update_attribute(:active, true)
    end
  end

  def down
  end
end
