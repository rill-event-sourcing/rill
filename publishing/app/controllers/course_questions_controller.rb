class CourseQuestionsController < ApplicationController

  before_action :set_param_objects
  before_action :set_redirect_cookie, only: [:index, :edit]
  before_action :set_breadcrumb

  def index
  end

  def preview
    render layout: 'preview'
  end

  def edit
  end

  def create
    @question = @course.questions.create
    redirect_to edit_question_path(@question)
  end

  def update
    set_line_inputs(@question, params[:line_inputs]) if params[:line_inputs]
    set_multiple_choice_inputs(@question, params[:multiple_choice_inputs]) if params[:multiple_choice_inputs]

    respond_to do |format|
      if @question.update_attributes(question_params)
        format.json { render json: @question.as_full_json }
      else
        format.json { render json: @question.errors, status: :unprocessable_entity }
      end
    end
  end

  def destroy
    @question.trash if @question
    redirect_to questions_path
  end

  def activate
    @question.activate
    redirect_to questions_path
  end

  def deactivate
    @question.deactivate
    redirect_to questions_path
  end

private

  def set_line_inputs(question, line_inputs_hash)
    line_inputs_hash.each do |id, values|
      line_input = question.line_inputs.where(id: id).first
      line_input.update_attributes(
        pre: values[:pre],
        post: values[:post],
        width: values[:width]
      )
      (values[:answers] || {}).each do |id,values|
        answer = line_input.answers.where(id: id).first
        answer.update_attributes(values)
      end
    end
  end

  def set_multiple_choice_inputs(question, multiple_choice_inputs_hash)
    multiple_choice_inputs_hash.each do |id, values|
      input = question.inputs.where(id: id).first
      (values[:choices] || {}).each do |id,values|
        values[:correct] ||= 0
        choice = input.choices.where(id: id).first
        choice.update_attributes(values)
      end
    end
  end

  def set_param_objects
    @course = Course.current
    @question = @course.questions.find_by_uuid(params[:id], false) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: "Course Questions", url: questions_path}
    @crumbs << {name: @question.to_param, url: question_path(@question)} if @question
  end

  def question_params
    params.require(:question).permit!
  end

end
