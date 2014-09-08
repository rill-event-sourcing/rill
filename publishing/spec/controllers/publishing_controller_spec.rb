require 'rails_helper'

RSpec.describe PublishingController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    @eq = create(:entry_quiz, course: @course)
    @question = create(:question, worked_out_answer: "", text: "_INPUT_1_", quizzable: @eq)
    @input1 = create(:line_input, inputable: @question)
    @answer1 = create(:answer, line_input: @input1, value: 'correct')
    controller.send :set_my_course

    @url = "#{StudyflowPublishing::Application.config.learning_server}/api/internal/course/#{ @course.id }"
    @headers = { 'Content-Type' => 'application/json' }
    @body = JSON.pretty_generate(@course.to_publishing_format)
  end

  describe "POST publish" do
    it "should publish the course material when there are no errors" do
      expect{post :publish}.not_to raise_error(Exception)
    end

    it "should throw an error when no course is selected" do
      session[:course_id] = nil
      controller.send :unset_my_course
      expect{post :publish}.to raise_error(Exception)
    end

    it "should publish the course material" do
      DelayedJob.delete_all
      post :publish
      expect(response).to redirect_to list_jobs_path
      expect(controller.flash[:notice]).to eq "Course '#{ @course }' was scheduled for publishing"
      expect(DelayedJob.count).to eq 1
    end
  end

  describe "GET check" do
    it "should warn when the latex rendering engine is not reachable" do
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body).and_return({foo:'bar'})
      mocked_response = HTTParty::Response.new({},response_object,{})
      post :check
      expect(assigns(:errors)).to include "Error with the connection to the Latex rendering engine"
      expect(HTTParty).to receive(:post).with("http://localhost:16000/", body: "2+2=4").and_return(mocked_response)
      post :check
      expect(assigns(:errors)).not_to include "Error with the connection to the Latex rendering engine"
    end
  end

  describe "POST publish async" do
    it "should publish the course material" do
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body).and_return({foo:'bar'})
      mocked_response = HTTParty::Response.new({},response_object,{})
      expect(HTTParty).to receive(:put).with(@url, headers: @headers, body: @body, timeout: 600).and_return(mocked_response)

      DelayedJob.delete_all
      post :publish
      worker = Delayed::Worker.new
      worker.work_off
      expect(DelayedJob.count).to eq 0
    end

    it "should warn when the course material cannot be published" do
      DelayedJob.delete_all
      post :publish
      worker = Delayed::Worker.new
      worker.work_off
      expect(DelayedJob.count).to eq 1
      delayed_job = DelayedJob.first
      expect(delayed_job.attempts).to eq 1
      expect(delayed_job.last_error).to match(/Connection refused/)
    end
  end

end
