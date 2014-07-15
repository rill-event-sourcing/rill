require 'rails_helper'

RSpec.describe ApplicationController, :type => :controller do

  before do
    @course = create(:course, name: 'Math', active: true)
  end

  describe "helper methods" do
    it "should set_my_course and reset_my_course" do
      session[:course_id] = @course.id
      expect(Course.current).to eq nil
      controller.send :set_my_course
      expect(Course.current).to eq @course

      session[:course_id] = nil
      controller.send :unset_my_course
      expect(Course.current).to eq nil
    end

  end

end
