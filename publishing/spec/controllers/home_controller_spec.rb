require 'rails_helper'

RSpec.describe HomeController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.set_my_course

    @url = "http://localhost:3000/api/internal/course/#{ @course.id }"
    @headers = { 'Content-Type' => 'application/json' }
    @body = JSON.pretty_generate(@course.as_json)
  end

  describe "GET index" do
    it "should show the home page" do
      get :index
      expect(response).to render_template('index')
      expect(response.status).to eq(200)
    end
  end

  describe "POST publish" do
    it "should publish the course material" do
      expect{post :publish}.not_to raise_error(Exception)
    end

    it "should throw an error when no course is selected" do
      session[:course_id] = nil
      controller.unset_my_course
      expect{post :publish}.to raise_error(Exception)
    end

    it "should publish the course material" do
      expect(HTTParty).to receive(:put).with(@url, headers: @headers, body: @body, timeout: 30)#.and_return({status: 200})
      post :publish
      expect(response).to redirect_to root_path
    end
  end

end
