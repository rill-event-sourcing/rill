require 'rails_helper'

RSpec.describe HomeController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course

    @url = "#{StudyflowPublishing::Application.config.learning_server}/api/internal/course/#{ @course.id }"
    @headers = { 'Content-Type' => 'application/json' }
    @body = JSON.pretty_generate(@course.to_publishing_format)
  end

  describe "GET index" do
    it "should show the home page" do
      get :index
      expect(response).to render_template('index')
      expect(response.status).to eq(200)
    end
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
      response_object = Net::HTTPOK.new('1.1', 200, 'OK')
      allow(response_object).to receive(:body).and_return({foo:'bar'})
      mocked_response = HTTParty::Response.new({},response_object,{})
      expect(HTTParty).to receive(:put).with(@url, headers: @headers, body: @body, timeout: 30).and_return(mocked_response)
      post :publish
      expect(response).to redirect_to root_path
      expect(controller.flash[:notice]).to eq "Course '#{ @course }' was succesfully published!"
    end

    it "should warn when the course material is not published" do
      response_object = Net::HTTPOK.new('1.1', 500, 'NOK')
      allow(response_object).to receive(:body).and_return({foo:'bar'})
      mocked_response = HTTParty::Response.new({},response_object,{})
      expect(HTTParty).to receive(:put).with(@url, headers: @headers, body: @body, timeout: 30).and_return(mocked_response)
      post :publish
      expect(response).to redirect_to root_path
      expect(controller.flash[:alert]).to eq "Course '#{ @course }' was NOT published!"
    end
  end

end
