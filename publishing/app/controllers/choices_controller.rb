class ChoicesController < ApplicationController
  before_action :set_param_objects

  def create
    @choice = @input.choices.build(value: '')
    if @choice.save
      render partial: 'edit', locals: {input: @input, choice: @choice}
    else
      return head :unprocessable_entity
    end
  end

  def destroy
    @choice.destroy if @choice
    render json: { status: :ok }
  end

  def moveup
    @choice.move_higher
    render json: { status: :ok }
  end

  def movedown
    @choice.move_lower
    render json: { status: :ok }
  end

  private

  def set_param_objects
    @question = Question.find_by_uuid(params[:question_id])
    @input = @question.inputs.find_by_uuid(params[:input_id])
    @choice = @input.choices.find_by_uuid(params[:id], false) if params[:id]
  end

end
