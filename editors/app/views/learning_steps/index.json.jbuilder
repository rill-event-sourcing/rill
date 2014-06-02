json.array!(@learning_steps) do |learning_step|
  json.extract! learning_step, :id, :name, :description, :chapter_id
  json.url learning_step_url(learning_step, format: :json)
end
