class AddNameToQuestions < ActiveRecord::Migration
  def change
    add_column :questions, :name, :string, limit: 5
  end
end
