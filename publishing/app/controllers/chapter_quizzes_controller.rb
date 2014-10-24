class ChapterQuizzesController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb

  def show
  end

  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @chapter_quiz = @chapter.chapter_quiz || @chapter.create_chapter_quiz
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: @chapter.title, url: chapter_sections_path(@chapter)}
    @crumbs << {name: "Chapter quiz", url: chapter_chapter_quiz_path(@chapter)}
  end


end
