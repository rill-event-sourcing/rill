class CreateChapters < ActiveRecord::Migration
  def change
    create_table :chapters do |t|
      t.references :course, index: true
      t.string :title
      t.text :description
      t.timestamps
    end
  end
end
