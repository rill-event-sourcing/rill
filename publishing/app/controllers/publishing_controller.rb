class PublishingController < ApplicationController

  def jobs
    @jobs = DelayedJob.all
  end

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

    course.publish!
    redirect_to list_jobs_path, notice: "Course '#{ course }' was scheduled for publishing"
  end

  def delete_job
    @job = DelayedJob.find(params[:id])
    @job.destroy if @job
    redirect_to list_jobs_path, notice: "Job was destroyed"
  end

end
