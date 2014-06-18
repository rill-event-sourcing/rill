class SubsectionsController < ApplicationController
  before_action :set_param_objects

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

end
