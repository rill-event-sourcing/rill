class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_action :check_authentication
  before_action :set_my_course
  after_action  :unset_my_course

private

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

  def check_authentication
    uuid = cookies["Studyflow"]
    if uuid
      StudyflowAuth.logged_in?(uuid)
    else
      cookies["Studyflow_redir_to"] = request.original_url
      redirect_to StudyflowPublishing::Application.config.auth_server
    end
  end
end
