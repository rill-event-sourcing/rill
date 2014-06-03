json.array!(@chapters) do |chapter|
  json.extract! chapter, :id, :name, :description, :course_id
  json.url chapter_url(chapter, format: :json)
end
