class InputsController < ApplicationController
  before_action :set_param_objects

  def create
    if params[:input_type] == 'line-input'
      @input = @question.line_inputs.build
    elsif params[:input_type] == 'multiple-choice'
      @input = @question.multiple_choice_inputs.build
    else
      raise "unknown input type"
    end
    if @input.save
      render partial: 'edit', locals: {input: @input}
    else
      return head :unprocessable_entity
    end
  end

  def destroy
    @input.destroy if @input
    render json: { status: :ok }
  end

private

  def set_param_objects
    @question = Question.find_by_uuid(params[:question_id])
    @input = @question.inputs.find_by_uuid(params[:id], false) if params[:id]
  end

end
