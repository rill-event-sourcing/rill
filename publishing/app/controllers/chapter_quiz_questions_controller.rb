class ChapterQuizQuestionsController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb, except: [:create]

  def index
  end

  def preview
    render layout: 'preview'
  end

  def edit
  end

  def create
    @question = @chapter_questions_set.questions.create
    redirect_to edit_chapter_chapter_quiz_chapter_questions_set_question_path(@chapter, @chapter_questions_set, @question)
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
    redirect_to chapter_chapter_quiz_path(@chapter)
  end


  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @chapter_quiz = @chapter.chapter_quiz || @chapter.create_chapter_quiz
    @chapter_questions_set = @chapter_quiz.chapter_questions_sets.find_by_uuid(params[:chapter_questions_set_id])
    @question = @chapter_questions_set.questions.find_by_uuid(params[:id]) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: "Chapter quiz", url: chapter_chapter_quiz_path(@chapter)}
    @crumbs << {name: @chapter_questions_set.title}
    @crumbs << {name: @question.to_param, url: chapter_chapter_quiz_chapter_questions_set_question_path(@chapter, @chapter_questions_set, @question)}
  end

  def question_params
    params.require(:question).permit!
  end

end
