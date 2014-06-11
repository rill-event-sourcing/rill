class SubsectionsController < ApplicationController

  before_action :set_course
  before_action :set_chapter
  before_action :set_section
  before_action :set_subsection, except: [:index, :new, :create]

  def create
    @subsection = @section.subsections.build(
      stars: params[:stars],
      title: '',
      description: '',
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

  def set_course
    @course = Course.current
  end

  def set_chapter
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
  end

  def set_section
    @section = @chapter.sections.find_by_uuid(params[:section_id])
  end

  def set_subsection
    @subsection = @section.subsections.find_by_uuid(params[:id], false)
  end

end
