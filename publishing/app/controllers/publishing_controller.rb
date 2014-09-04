class PublishingController < ApplicationController

  def check
    @errors = []
    begin
      response = HTTParty.post("http://localhost:16000/", body: "2+2=4")
    rescue
      @errors << "Error with the connection to the Latex rendering engine"
    end
    @errors << Course.current.errors_when_publishing
  end


  def publish
    course = Course.current
    throw Exception.new("Publishing without course selected!") unless course

    if course.errors_when_publishing.any?
      redirect_to check_course_path
    else
      course.publish!
      redirect_to root_path, notice: "Course '#{ course }' was scheduled for publishing"
    end
  end

  def jobs
    @jobs = DelayedJob.all
  end

end
