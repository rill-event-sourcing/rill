module ExtraExampleActions
  extend ActiveSupport::Concern

  included do
  end

  private

  def set_extra_examples(section, extra_examples_hash)
    extra_examples_hash.each do |id, values|
      extra_example = section.extra_examples.where(id: id).first
      extra_example.update_attributes(title: values[:title],
                                      default_open: values[:default_open],
                                      content: values[:content])
    end
  end
end
