class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :check_authentication
  before_action :set_my_course
  after_action  :unset_my_course

  def check_authentication
    unless StudyflowAuth.logged_in?
      redirect_to "http://login.studyflow.nl:3000"
    end
  end

  def set_my_course
    Course.current = Course.where(id: session[:course_id]).first
  end

  def unset_my_course
    Course.current = nil
  end

  def set_crumb(crumb_hash)
    @crumbs ||= []
    @crumbs << crumb_hash
  end

private

  def logged_in?
    false
  end
end
