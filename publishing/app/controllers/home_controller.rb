class HomeController < ApplicationController

  def index
  end

  def publish
    course = Course.current
    throw "Publishing without course selected!" unless course

    course_json = JSON.pretty_generate(course.as_json)
    url = "http://localhost:3001/api/internal/course/#{ course.id }"
    # p "Putting Course material to: #{ url }"

    begin
      publish_response =  HTTParty.put(url,
        headers: { 'Content-Type' => 'application/json' },
        body: course_json
      )
      parsed_response = JSON.parse(response)
    rescue Errno::ECONNREFUSED
    end

    throw parsed_response
    if parsed_response && parsed_response
      redirect_to root_path, notice: "Course '#{ course }' was succesfully published!"
    else
      flash[:alert] = "Course '#{ course }' was NOT published!"
      redirect_to root_path
    end
  end

end
