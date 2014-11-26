module InputActions
  extend ActiveSupport::Concern

  included do
  end

  private

  def set_line_inputs(question, line_inputs_hash)
    line_inputs_hash.each do |id, values|
      line_input = question.line_inputs.where(id: id).first
      if line_input
        line_input.update_attributes(
          style: values[:style],
          width: values[:width],
          prefix: values[:prefix],
          suffix: values[:suffix]
        )
        (values[:answers] || {}).each do |id, values|
          answer = line_input.answers.where(id: id).first
          answer.update_attributes(values) if answer
        end
      end
    end
  end

  def set_multiple_choice_inputs(question, multiple_choice_inputs_hash)
    multiple_choice_inputs_hash.each do |id, values|
      input = question.inputs.where(id: id).first
      if input
        (values[:choices] || {}).each do |id,values|
          values[:correct] ||= 0
          choice = input.choices.where(id: id).first
          choice.update_attributes(values) if choice
        end
      end
    end
  end

end
