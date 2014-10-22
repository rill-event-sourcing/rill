class ChapterQuestionsSetsController < ApplicationController


  
  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @chapter_quiz = @chapter.chapter_quiz || @chapter.create_chapter_quiz
  end

  def entry_quiz_params
    params.require(:chapter_quiz).permit()
  end

end
