class StripWhitespaceFromAnswers < ActiveRecord::Migration
  # This is destructive!
  class Answer < ActiveRecord::Base
  end

  def up
    Answer.all.each do |answer|
      answer.update_attribute(:value, answer.value.strip)
    end
  end

  def down
  end
end
