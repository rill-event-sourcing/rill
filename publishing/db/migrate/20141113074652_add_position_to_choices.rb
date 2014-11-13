class AddPositionToChoices < ActiveRecord::Migration
  class Choice < ActiveRecord::Base
  end

  class Input < ActiveRecord::Base
  end

  class MultipleChoiceInput < Input
  end

  def put_default_position_on_choices
    MultipleChoiceInput.all.each do |mc|
      p "#{mc.id}"
      mc.choices.each_with_index do |choice, index|
        choice.update_attribute(:position, index)
      end
    end
  end

  def up
    add_column :choices, :position, :integer, limit: 3
    Choice.reset_column_information
    put_default_position_on_choices
  end

  def down
    remove_column :choices, :position
  end

end
