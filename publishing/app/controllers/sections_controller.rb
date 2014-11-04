class SectionsController < ApplicationController
  before_action :set_param_objects, except: [:search]
  before_action :set_redirect_cookie, only: [:index, :show, :new, :edit]
  before_action :set_breadcrumb, except: [:create, :search]

  def index
    redirect_to @chapter
  end

  def show
  end

  def new
    @section = Section.new
  end

  def edit
  end

  def create
    @section = @chapter.sections.build(section_params)
    if @section.save
      redirect_to [@chapter, @section]
    else
      render 'new'
    end
  end

  def update
    if @section.update(section_params)
      redirect_to chapter_sections_path(@chapter), notice: 'Section was successfully updated.'
    else
      render :show
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


  def toggle_activation
    @section.active? ? @section.deactivate : @section.activate
    redirect_to chapter_section_path(@chapter,@section)
  end


  def moveup
    @section.move_higher
    redirect_to chapter_sections_path, notice: 'Section was successfully moved up.'
  end

  def movedown
    @section.move_lower
    redirect_to chapter_sections_path, notice: 'Section was successfully moved down.'
  end

  def search
    @course = Course.current
    @crumbs = [{name: @course.name, url: root_path}, {name: "Search Section"}]
    @section ||= Section.new
    if request.post?
      begin
        @section = Section.find_by_uuid(params[:search_id], false)
        @section ||= Section.where(id: params[:search_id]).first
      rescue # PG::InvalidTextRepresentation
      end
      if @section
        redirect_to chapter_section_path(@section.chapter, @section)
      else
        flash[:error] = "Section not found for: '#{params[:search_id]}'"
      end
    end
    @section ||= Section.new
  end


  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:id]) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: @section.title, url: chapter_section_path(@chapter, @section)} if @section
    @crumbs << {name: "New section", url: ""} if action_name == "new"
  end

  def section_params
    params.require(:section).permit!
  end

end
