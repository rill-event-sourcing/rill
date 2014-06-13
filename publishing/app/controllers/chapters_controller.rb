class ChaptersController < ApplicationController

  before_action :set_course
  before_action :set_chapter, except: [:index, :new, :create]
  before_action :set_breadcrumb, except: [:index, :new, :create]

  def index
    if @course
      @chapters = @course.chapters
    else
      flash.now[:alert] = 'Please select a course first'
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
    @chapter.trash
    redirect_to chapters_path, notice: 'Chapter was successfully destroyed.'
  end

  def activate
    @chapter.activate
    redirect_to chapters_path
  end

  def deactivate
    @chapter.deactivate
    redirect_to chapters_path
  end

  def moveup
    @chapter.move_higher
    redirect_to chapters_path, notice: 'Chapter was successfully moved up.'
  end

  def movedown
    @chapter.move_lower
    redirect_to chapters_path, notice: 'Chapter was successfully moved down.'
  end

private

  def set_course
    @course = Course.current
  end

  def set_breadcrumb
    @crumbs = [{name: @chapter.title, url: chapter_sections_path(@chapter)}]
  end

  def set_chapter
    @chapter = @course.chapters.find_by_uuid(params[:id])
  end

  def chapter_params
    params.require(:chapter).permit(:title, :description)
  end

end
