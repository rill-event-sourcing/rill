class QuestionsController < ApplicationController

before_action :set_param_objects
before_action :set_breadcrumb

def index
end

def edit
end

private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @section = @chapter.sections.find_by_uuid(params[:section_id])
    @question = @section.questions.find_by_uuid(params[:id]) if params[:id]
  end

  def set_breadcrumb
    @crumbs = [{name: @chapter.title, url: chapter_sections_path(@chapter)},{name: @section.title, url: chapter_section_path(@chapter, @section)}, {name: "Questions", url: chapter_section_subsections_path(@chapter, @section)}]
  end

end
