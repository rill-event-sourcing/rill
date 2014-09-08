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

end
