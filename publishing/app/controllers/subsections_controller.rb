class SubsectionsController < ApplicationController

  before_action :set_course
  before_action :set_chapter
  before_action :set_section
  before_action :set_subsection, except: [:index, :new, :create]

  def create
    Rails.logger.debug "xxxxxx #{ params }"
    @subsection = @section.subsections.build(
      stars: params[:stars],
      title: '',
      description: ''
    )
    if @subsection.save
      @star = @subsection.stars
      @index = @subsection.id
      render partial: 'edit'
    else
      Rails.logger.debug 'xxxxxx'+ @subsection.errors.full_messages.join(', ')
      return head :unprocessable_entity
    end
  end

  def destroy
    @subsection.destroy if @subsection
    # if @subsection.destroy
      render json: { status: :ok } #, count: @section.subsections.find_by_star(@subsection.stars).count }
    # else
    #   return head :unprocessable_entity
    # end
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
