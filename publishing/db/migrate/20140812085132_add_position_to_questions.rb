class AddPositionToQuestions < ActiveRecord::Migration
  def change
    add_column :questions, :position, :integer, limit: 2
  end
end
