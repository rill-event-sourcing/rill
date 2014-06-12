require 'rails_helper'

RSpec.describe ApplicationController, :type => :controller do

  before do
    @course = create(:course, name: 'Math', active: true)
  end

  describe "helper methods" do
    it "should set_my_course and rest_my_course" do
      session[:course_id] = @course.id
      expect(Course.current).to eq nil
      controller.set_my_course
      expect(Course.current).to eq @course

      session[:course_id] = nil
      controller.reset_my_course
      expect(Course.current).to eq nil
    end

    it "should set_crumb" do
      crumb_hash1 = {link: '1234'}
      crumbs = controller.set_crumb(crumb_hash1)
      expect(assigns(:crumbs)).to eq [crumb_hash1]

      crumb_hash2 = {link2: '5678'}
      controller.set_crumb(crumb_hash2)
      expect(assigns(:crumbs)).to eq [crumb_hash1, crumb_hash2]
    end
  end

end
