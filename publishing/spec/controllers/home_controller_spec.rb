require 'rails_helper'

RSpec.describe HomeController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
  end

  describe "GET index" do
    it "should show the home page" do
      get :index
      expect(response).to render_template('index')
      expect(response.status).to eq(200)
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

end
