class AddMeijerinkCriteriaToSections < ActiveRecord::Migration
  def change
    add_column :sections, :meijerink_criteria, :string
  end
end
