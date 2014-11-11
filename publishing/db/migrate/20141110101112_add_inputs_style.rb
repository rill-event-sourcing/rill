class AddInputsStyle < ActiveRecord::Migration
  def change
    add_column :inputs, :style, :string, default: "big"
  end
end
