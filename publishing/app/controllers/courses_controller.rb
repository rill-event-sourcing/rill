class CoursesController < ApplicationController

  def select
    # p "zzzzzzzzzzzzzz #{ params }"
    if params[:course] && params[:course][:id] && params[:course][:id] != ""
      @course = Course.find(params[:course][:id])
      Course.current = @course
      session[:course_id] = @course.id
      # p "zzzzzzzzzzzzz selected: #{ @course }"
    else
      Course.current = nil
      session[:course_id] = nil
      # p "zzzzzzzzzzzzz UNselected: #{ @course }"
    end
    redirect_to root_path
  end

end
