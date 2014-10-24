require 'rails_helper'

RSpec.describe ChapterQuizzesController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @chapter = create(:chapter, course: @course)

  end

  describe "GET show" do
    it "should render the show template" do
      get :show, chapter_id: @chapter
      expect(response).to render_template('show')
    end
  end

end
