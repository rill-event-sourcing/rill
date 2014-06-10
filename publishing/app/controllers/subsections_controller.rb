class SubsectionsController < ApplicationController

  before_action :set_course
  before_action :set_chapter
  before_action :set_section
  before_action :set_subsection

  def destroy
    if @subsection.destroy
      render json: { status: :ok, count: @section.subsections.find_by_star(@subsection.stars).count }
    else
      return head :unprocessable_entity
    end
  end


def update
  respond_to do |format|
    if @section.update(section_params)
      format.html { redirect_to chapter_section_path(@chapter, @section), notice: 'Section was successfully updated.' }
      format.json { render json: @section.as_full_json }
    else
      format.html { render :show }
      format.json { render json: @section.errors, status: :unprocessable_entity }
    end
  end
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
    @subsection = @section.subsections.find_by_uuid(params[:id])
  end

end
