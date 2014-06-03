class AddDeletedCourses < ActiveRecord::Migration
  def change
    add_column :courses, :deleted_at, :datetime
  end
end
