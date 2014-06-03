class AddDeletedChapters < ActiveRecord::Migration
  def change
    add_column :chapters, :deleted_at, :datetime
  end
end
