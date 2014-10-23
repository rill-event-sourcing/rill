class ChapterQuestionsSetsController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb

  def index
    @chapter_questions_sets = @chapter_quiz.chapter_questions_sets
  end

  def show
  end

  def new
    @chapter_questions_set = @chapter_quiz.chapter_questions_sets.build
  end

  def edit
  end

  def create
    @chapter_questions_set = @chapter_quiz.chapter_questions_sets.build(questions_set_params)
    if @chapter_questions_set.save
      redirect_to chapter_chapter_quiz_path(@chapter), notice: "Questions set succesfully created"
    else
      render :new
    end
  end

  def update
    if @chapter_questions_set.update(questions_set_params)
      redirect_to chapter_chapter_quiz_path(@chapter), notice: "Questions set succesfully created"
    else
      render :edit
    end
  end

  def destroy
    @chapter_questions_set.destroy
    redirect_to chapter_chapter_quiz_path(@chapter), notice: 'Questions set was successfully destroyed.'
  end

  def moveup
    @chapter_questions_set.move_higher
    redirect_to chapter_chapter_quiz_path(@chapter), notice: 'Questions set was successfully moved up.'
  end

  def movedown
    @chapter_questions_set.move_lower
    redirect_to chapter_chapter_quiz_path(@chapter), notice: "Questions set was successfully moved down."
  end


  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @chapter_quiz = @chapter.chapter_quiz || @chapter.create_chapter_quiz
    @chapter_questions_set = @chapter_quiz.chapter_questions_sets.find_by_uuid(params[:id]) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: "Chapter quiz", url: chapter_chapter_quiz_path(@chapter)}
    if @chapter_questions_set
      @crumbs << {name: @chapter_questions_set.title, url: chapter_chapter_quiz_chapter_questions_set_path(@chapter,@chapter_questions_set)}
    else
      @crumbs << {name: "New question set", url: ""}
    end
  end


  def questions_set_params
    params.require(:chapter_questions_set).permit(:title)
  end

end
