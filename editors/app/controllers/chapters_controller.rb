class ChaptersController < ApplicationController
  before_action :set_course
  before_action :set_chapter, only: [:show, :edit, :update, :destroy]

  def index
    if @course
      @chapters = @course.chapters
    else
      redirect_to root_path, alert: 'Please select a course first'
    end
  end

  def show
  end

  def new
    @chapter = @course.chapters.build
  end

  def edit
  end

  def create
    @chapter = @course.chapters.build(chapter_params)
    if @chapter.save
      redirect_to chapters_path, notice: 'Chapter was successfully created.'
    else
      render :new
    end
  end

  def update
    if @chapter.update(chapter_params)
      redirect_to @chapter, notice: 'Chapter was successfully updated.'
    else
      render :edit
    end
  end

  def destroy
    @chapter.destroy
    redirect_to @course, notice: 'Chapter was successfully destroyed.'
  end

private

  def set_course
    @course = Course.current
  end

  def set_chapter
    @chapter = @course.chapters.find(params[:id])
  end

  def chapter_params
    params.require(:chapter).permit(:title, :description, :course_id)
  end



end
