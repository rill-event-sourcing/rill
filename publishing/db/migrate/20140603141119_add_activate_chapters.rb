class AddActivateChapters < ActiveRecord::Migration
  def change
    add_column :chapters, :active, :boolean, default: false
  end
end
