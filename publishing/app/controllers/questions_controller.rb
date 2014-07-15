class QuestionsController < ApplicationController

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
    @question = @section.questions.create
    redirect_to edit_chapter_section_question_path(@chapter, @section, @question)
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
    redirect_to chapter_section_questions_path(@chapter,@section)
  end

  def activate
    @question.activate
    redirect_to chapter_section_questions_path(@chapter,@section)
  end

  def deactivate
    @question.deactivate
    redirect_to chapter_section_questions_path(@chapter,@section)
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
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:section_id])
    @question = @section.questions.find_by_uuid(params[:id], false) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: @section.title, url: chapter_section_path(@chapter, @section)}
    @crumbs << {name: "Questions", url: chapter_section_questions_path(@chapter,@section)}
    @crumbs << {name: @question.to_param, url: chapter_section_question_path(@chapter,@section,@question)} if @question
  end

  def question_params
    params.require(:question).permit!
  end

end
