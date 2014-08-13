class AddRemedialToChapters < ActiveRecord::Migration
  def change
    add_column :chapters, :remedial, :boolean, default: false
  end
end
