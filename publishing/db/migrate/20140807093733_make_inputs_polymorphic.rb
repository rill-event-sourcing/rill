class MakeInputsPolymorphic < ActiveRecord::Migration
  def up
    rename_column :inputs, :question_id, :inputable_id
    add_column :inputs, :inputable_type, :string
    execute "UPDATE inputs SET inputable_type = 'Question';"
  end

  def down
    rename_column :inputs, :inputable_id, :question_id
    remove_column :inputs, :inputable_type
  end
end
