class HomeController < ApplicationController

  before_action :set_redirect_cookie, only: [:index]
  before_action :set_breadcrumb

  def health_check
    # test being up and sane
    if Course.first # getting a Course from the database is an indication
      render json: { status: 'up' }
    else
      return head :service_unavailable
    end
  end

  def index
  end

  def publish
    course = Course.current
    throw Exception.new("Publishing without course selected!") unless course

    course_json = JSON.pretty_generate(course.to_publishing_format)
    publishing_url = "#{StudyflowPublishing::Application.config.learning_server}/api/internal/course/#{ course.id }"

    if course.errors_when_publishing.any?
      flash[:alert] = "Please fix the errors before publishing"
      redirect_to root_path
    else
      begin
        publish_response =  HTTParty.put(publishing_url,
                                         headers: { 'Content-Type' => 'application/json' },
                                         body: course_json,
                                         timeout: 30)
      rescue Errno::ECONNREFUSED
        failed_publish_msg = "Connection refused while publishing to: #{ publishing_url }"
      rescue Net::ReadTimeout
        failed_publish_msg = "Timeout while publishing to: #{ publishing_url }"
      rescue Exception => ex
        failed_publish_msg = "Unknow exception while publishing to: #{ publishing_url }: #{ ex }"
      end

      if publish_response && publish_response.code == 200
        redirect_to root_path, notice: "Course '#{ course }' was succesfully published!"
      else
        flash[:alert] = "Course '#{ course }' was NOT published!"
        failed_publish_msg ||= "Response code was: #{ publish_response.code }"
        flash[:notice] = failed_publish_msg
        redirect_to root_path
      end
    end
  end

  private

  def set_breadcrumb
    @crumbs = [{name: Course.current.name, url: root_path}] if Course.current
  end
end
