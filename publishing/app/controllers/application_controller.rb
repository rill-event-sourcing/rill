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

  def set_redirect_cookie
  end

  def really_set_redirect_cookie
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
    user_id = cookies["studyflow_session"]
    if user_id && StudyflowAuth.logged_in?(user_id)
      return true
    else
      redirect_to StudyflowPublishing::Application.config.auth_server
    end
  end

def set_line_inputs(question, line_inputs_hash)
    line_inputs_hash.each do |id, values|
      line_input = question.line_inputs.where(id: id).first
      line_input.update_attributes(
        prefix: values[:prefix],
        suffix: values[:suffix],
        width: values[:width]
      )
      (values[:answers] || {}).each do |id,values|
        answer = line_input.answers.where(id: id).first
        answer.update_attributes(values)
      end
    end
  end

  def set_multiple_choice_inputs(question, multiple_choice_inputs_hash)
    multiple_choice_inputs_hash.each do |id, values|
      input = question.inputs.where(id: id).first
      (values[:choices] || {}).each do |id,values|
        values[:correct] ||= 0
        choice = input.choices.where(id: id).first
        choice.update_attributes(values)
      end
    end
  end


end
