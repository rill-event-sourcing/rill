class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :set_my_course
  after_action  :reset_my_course

  def set_my_course
    Course.current = Course.where(id: session[:course_id]).first
  end

  def reset_my_course
    Course.current = nil
  end

  def set_crumb(crumb_hash)
    @crumbs ||= []
    @crumbs << crumb_hash
  end

end
