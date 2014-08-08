class AddToolsQuestions < ActiveRecord::Migration
  def change
    add_column :questions, :tools, :string
  end
end
