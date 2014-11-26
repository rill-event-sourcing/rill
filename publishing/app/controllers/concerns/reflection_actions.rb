module ReflectionActions
  extend ActiveSupport::Concern

  included do
  end

  private

  def set_reflections(section, reflections_hash)
    reflections_hash.each do |id, values|
      reflection = section.reflections.where(id: id).first
      if reflection
        reflection.update_attributes(content: values[:content],
                                     answer: values[:answer])
      end
    end
  end
end
