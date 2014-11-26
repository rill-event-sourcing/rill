module ExtraExampleActions
  extend ActiveSupport::Concern

  included do
  end

  private

  def set_extra_examples(section, extra_examples_hash)
    extra_examples_hash.each do |id, values|
      extra_example = section.extra_examples.where(id: id).first
      if extra_example
        extra_example.update_attributes(title: values[:title],
                                        default_open: values[:default_open].to_i,
                                        content: values[:content])
      end
    end
  end
end
