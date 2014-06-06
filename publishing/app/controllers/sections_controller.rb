class SectionsController < ApplicationController

  before_action :set_course
  before_action :set_chapter
  before_action :set_section, except: [:index, :new, :create]
  before_action :set_breadcrumb, except: [:index, :new, :create]

  def preview
    @subsections = @section.subsections.find_by_star(params[:star])
    render layout: 'preview'
  end

  def index
    redirect_to @chapter
  end

  def show
    @subsections = @section.subsections.group_by(&:stars)
  end

  # def new
  #   @section = @chapter.sections.build
  # end

  # def edit
  # end

  # def create
  #   @section = @chapter.sections.build(section_params)
  #   if @section.save
  #     redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully created.'
  #   else
  #     render :new
  #   end
  # end

  def update
    respond_to do |format|
      if @section.update(section_params)
        format.json { render json: @section.as_full_json, status: :ok }
      else
        format.json { render json: @section.errors, status: :unprocessable_entity }
      end
    end
  end

  def destroy
    @section.trash
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

  def set_breadcrumb
    set_crumb({name: @chapter.title, url: chapter_sections_path(@chapter)})
    set_crumb({name: @section.title, url: chapter_section_path(@chapter, @section)})
  end

  def set_course
    @course = Course.current
  end

  def set_chapter
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
  end

  def set_section
    @section = @chapter.sections.find_by_uuid(params[:id])
  end

  def section_params
    params.require(:section).permit(:title, :description)
  end

end
