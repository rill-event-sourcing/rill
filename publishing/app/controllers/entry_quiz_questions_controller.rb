class EntryQuizQuestionsController < ApplicationController
  include InputActions

  before_action :set_param_objects
  before_action :set_redirect_cookie, only: [:index, :edit]
  before_action :set_breadcrumb

  def index
  end

  def preview_content
    render layout: 'preview_html'
  end

  def error_content
    render partial: 'error_content'
  end

  def edit
  end

  def create
    @question = @entry_quiz.questions.create
    redirect_to edit_entry_quiz_question_path(@question)
  end

  def update
    params[:question] ||= {}
    params[:question][:tools] ||= {}
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
    redirect_to entry_quiz_questions_path
  end

  def activate
    @question.activate
    redirect_to entry_quiz_questions_path
  end


  def toggle_activation
    @question.active? ? @question.deactivate : @question.activate
    redirect_to edit_entry_quiz_question_path(@question)
  end


  def deactivate
    @question.deactivate
    redirect_to entry_quiz_questions_path
  end

  def moveup
    @question.move_higher
    redirect_to entry_quiz_questions_path, notice: 'Chapter was successfully moved up.'
  end

  def movedown
    @question.move_lower
    redirect_to entry_quiz_questions_path, notice: 'Chapter was successfully moved down.'
  end

  private

  def set_param_objects
    @course = Course.current
    @entry_quiz = @course.entry_quiz
    @question = @entry_quiz.questions.find_by_uuid(params[:id], false) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: "Entry Quiz", url: entry_quiz_path}
    @crumbs << {name: "Questions", url: entry_quiz_questions_path}
    @crumbs << {name: @question.to_param, url: entry_quiz_question_path(@question)} if @question
  end

  def question_params
    params.require(:question).permit!
  end

end
