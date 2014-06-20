class AnswersController < ApplicationController
  before_action :set_param_objects

  def create
    @answer = @input.answers.build(
      value: ''
    )
    if @answer.save
      render partial: 'edit', locals: {input: @input, answer: @answer}
    else
      return head :unprocessable_entity
    end
  end

  def destroy
    @answer.destroy if @answer
    render json: { status: :ok }
  end

private

  def set_param_objects
    @question = Question.find_by_uuid(params[:question_id])
    @input = @question.inputs.find_by_uuid(params[:input_id])
    @answer = @input.answers.find_by_uuid(params[:id], false) if params[:id]
  end

end
