class AddActivateCourses < ActiveRecord::Migration
  def change
    add_column :courses, :active, :boolean, default: false
  end
end
