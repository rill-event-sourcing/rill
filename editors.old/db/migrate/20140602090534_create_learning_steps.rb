class CreateLearningSteps < ActiveRecord::Migration
  def change
    create_table :learning_steps do |t|
      t.string :name
      t.text :description
      t.references :chapter, index: true

      t.timestamps
    end
  end
end
