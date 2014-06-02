class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :get_my_course

  def get_my_course
    Course.current = Course.find(session[:course_id]) if session[:course_id].to_i > 0
    @my_course = Course.current || Course.new
  end

end
