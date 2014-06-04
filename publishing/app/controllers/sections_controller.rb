class SectionsController < ApplicationController
  before_action :set_course
  before_action :set_chapter
  before_action :set_section, except: [:index, :new, :create]

  def index
  end

  def show
  end

  def new
    @section = @chapter.sections.build
  end

  def edit
  end

  def create
    @section = @chapter.sections.build(section_params)
    if @section.save
      redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully created.'
    else
      render :new
    end
  end

  def update
    if @section.update(section_params)
      redirect_to [@chapter, @section], notice: 'Section was successfully updated.'
    else
      render :edit
    end
  end

  def destroy
    @chapter.trash
    redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully destroyed.'
  end

  def activate
    @section.activate
    redirect_to chapter_sections_path(@chapter)
  end

  def deactivate
    @section.deactivate
    redirect_to chapter_sections_path(@chapter)
  end

  def moveup
    @section.move_higher
    redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully moved up.'
  end

  def movedown
    @section.move_lower
    redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully moved down.'
  end

private

   def set_course
     @course = Course.current
   end
#
   def set_chapter
     @chapter = @course.chapters.find(params[:chapter_id])
   end

   def set_section
     @section = @chapter.sections.find(params[:id])
   end

   def section_params
     params.require(:section).permit(:title, :description, :chapter_id)
   end
end
