class SectionsController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb, except: [:index, :new, :create]

  def index
    redirect_to @chapter
  end

  def show
    @all_subsections = @section.subsections.group_by(&:stars)
  end

  def new
    @section = Section.new
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
    respond_to do |format|
      subsections(params[:subsections]) if params[:subsections]
      if @section.update(section_params)
        format.html { redirect_to chapter_section_path(@chapter, @section), notice: 'Section was successfully updated.' }
        format.json { render json: @section.as_full_json }
      else
        format.html { render :show }
        format.json { render json: @section.errors, status: :unprocessable_entity }
      end
    end
  end

  def preview
    @star = params[:star]
    @subsections = @section.subsections.find_by_star(@star)
    render layout: 'preview'
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
    redirect_to chapter_sections_path, notice: 'Section was successfully moved up.'
  end

  def movedown
    @section.move_lower
    redirect_to chapter_sections_path, notice: 'Section was successfully moved down.'
  end

private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:id]) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @chapter.title, url: chapter_sections_path(@chapter)},{name: @section.title, url: chapter_section_path(@chapter, @section)}]
  end

  def section_params
    params.require(:section).permit(:title, :description)
  end

  def subsections(subsection_hash)
    subsection_hash.each do |stars, new_subsections|
      new_subsections.values.each_with_index do |new_subsection, index|
        my_subsection = @section.subsections.find(new_subsection['id'])
        my_subsection.update_attributes(
          title: new_subsection['title'],
          text: new_subsection['text'],
          position: index
        )
      end
    end
    @section.updated_at= Time.now
  end

end
