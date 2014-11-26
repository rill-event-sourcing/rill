class CreateImages < ActiveRecord::Migration
  def change
    create_table :images do |t|
      t.string   :url
      t.string   :sha
      t.string   :status
      t.datetime :checked_at
      t.integer  :width,  limit: 4
      t.integer  :height, limit: 4
      t.timestamps
    end
  end
end
