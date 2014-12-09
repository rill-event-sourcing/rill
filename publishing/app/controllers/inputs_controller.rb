class InputsController < ApplicationController
  before_action :set_param_objects

  def create
    if params[:input_type] == 'line-input'
      @input = @inputable.line_inputs.build
      @input.answers.build
    elsif params[:input_type] == 'multiple-choice'
      @input = @inputable.multiple_choice_inputs.build
      @input.choices.build
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
    if params[:question_id]
      @inputable = @question = Question.find_by_uuid(params[:question_id])
      @input = @question.inputs.find_by_uuid(params[:id], false) if params[:id]
    end
    if params[:section_id]
      @inputable = @section = Section.find_by_uuid(params[:section_id])
      @input = @section.inputs.find_by_uuid(params[:id], false) if params[:id]
    end

  end

end
