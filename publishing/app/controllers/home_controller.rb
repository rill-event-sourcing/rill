class HomeController < ApplicationController

  before_action :set_redirect_cookie, only: [:index]
  before_action :set_breadcrumb

  def index
  end

  def publish
    course = Course.current
    throw Exception.new("Publishing without course selected!") unless course

    course_json = JSON.pretty_generate(course.as_json)
    url = "http://localhost:3000/api/internal/course/#{ course.id }"

    begin
      publish_response =  HTTParty.put(url,
                                       headers: { 'Content-Type' => 'application/json' },
                                       body: course_json,
                                       timeout: 30
                                       )
    rescue Errno::ECONNREFUSED
    rescue Net::ReadTimeout
    end

    if publish_response && publish_response.code == 200
      redirect_to root_path, notice: "Course '#{ course }' was succesfully published!"
    else
      flash[:alert] = "Course '#{ course }' was NOT published!"
      redirect_to root_path
    end
  end

  private

  def set_breadcrumb
    @crumbs = [{name: Course.current.name, url: root_path}] if Course.current
  end
end
