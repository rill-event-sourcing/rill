class SubsectionsController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb

  def index
    @all_subsections = @section.subsections.group_by(&:stars)
  end

  def preview
    @star = params[:star]
    @subsections = @section.subsections.find_by_star(@star)
    render layout: 'preview'
  end

  def create
    @subsection = @section.subsections.build(
      stars: params[:stars],
      title: '',
      text: '',
      position: params[:position]
    )
    if @subsection.save
      @section.subsections.where(["id <> ? AND stars = ? AND position >= ?", @subsection.id, @subsection.stars, @subsection.position]).update_all("position=position+1")
      @star = @subsection.stars
      @index = @subsection.id
      render partial: 'edit'
    else
      return head :unprocessable_entity
    end
  end

  def save
    respond_to do |format|
      subsections(params[:subsections])# if params[:subsections]
      if @section.save
        format.json { render json: @section.as_full_json }
      else
        format.json { render json: @section.errors, status: :unprocessable_entity }
      end
    end
  end

  def destroy
    @subsection.destroy if @subsection
    render json: { status: :ok }
  end

private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:section_id])
    @subsection = @section.subsections.find_by_uuid(params[:id]) if params[:id]
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

  def set_breadcrumb
    @crumbs = [{name: @chapter.title, url: chapter_sections_path(@chapter)},{name: @section.title, url: chapter_section_path(@chapter, @section)}, {name: "Subsections", url: chapter_section_subsections_path(@chapter, @section)}]
  end

end
