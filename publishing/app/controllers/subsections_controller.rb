class SubsectionsController < ApplicationController
  include InputActions, ReflectionActions

  before_action :set_param_objects
  before_action :set_redirect_cookie, only: [:index]
  before_action :set_breadcrumb

  def index
  end

  def preview
    render layout: 'preview'
  end

  def preview_content
    render layout: 'preview_html'
  end

  def create
    @subsection = @section.subsections.build(title: '',
                                             text: '',
                                             position: params[:position])
    if @subsection.save
      @section.subsections.where(["id <> ? AND position >= ?", @subsection.id, @subsection.position]).update_all("position=position+1")
      @index = @subsection.id
      render partial: 'edit'
    else
      return head :unprocessable_entity
    end
  end

  def save
    set_line_inputs(@section, params[:line_inputs]) if params[:line_inputs]
    set_multiple_choice_inputs(@section, params[:multiple_choice_inputs]) if params[:multiple_choice_inputs]
    set_reflections(@section, params[:reflections]) if params[:reflections]

    respond_to do |format|
      subsections(params[:subsections]) if params[:subsections]
      if @section.save
        format.json { render json: @section.as_full_json }
      else
        format.json { render json: @section.errors, status: :unprocessable_entity }
      end
    end
  end

  # def moveup
  #   @subsection.move_higher
  #   render json: { status: :ok }
  # end

  # def movedown
  #   @subsection.move_lower
  #   render json: { status: :ok }
  # end

  def destroy
    @subsection.destroy if @subsection
    render json: { status: :ok }
  end

  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:section_id])
    @subsection = @section.subsections.find_by_uuid(params[:id], false) if params[:id]
  end

  def subsections(subsection_hash)
    subsection_hash.each do |subsection_id, new_subsection|
      my_subsection = @section.subsections.find(subsection_id)
      my_subsection.update_attributes(
                                      title: new_subsection['title'],
                                      text: new_subsection['text'],
                                      position: new_subsection['position'])
    end
    @section.updated_at= Time.now
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: @section.title, url: chapter_section_path(@chapter, @section)}
    @crumbs << {name: "Subsections", url: chapter_section_subsections_path(@chapter, @section)}
  end

end
