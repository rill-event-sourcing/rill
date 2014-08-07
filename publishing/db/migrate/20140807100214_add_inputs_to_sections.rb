class AddInputsToSections < ActiveRecord::Migration
  def change
    add_column :sections, :max_inputs, :integer, limit: 2
  end
end
