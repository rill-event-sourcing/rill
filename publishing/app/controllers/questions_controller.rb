class QuestionsController < ApplicationController

before_action :set_param_objects

def index
end

private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:section_id])
  end

end
