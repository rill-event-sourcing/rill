class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  # before_action :check_authentication, unless: -> { Rails.env == 'test' }
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

  def set_redirect_cookie
    if StudyflowPublishing::Application.config.cookie_domain == "localhost"
      cookies["studyflow_redir_to"] = { value: request.original_url }
    else
      cookies["studyflow_redir_to"] = {
        value: request.original_url,
        domain: StudyflowPublishing::Application.config.cookie_domain
      }
    end
  end

  def check_authentication
    uuid = cookies["studyflow_session"]
    if uuid && StudyflowAuth.logged_in?(uuid)
      return true
    else
      redirect_to StudyflowPublishing::Application.config.auth_server
    end
  end

end
