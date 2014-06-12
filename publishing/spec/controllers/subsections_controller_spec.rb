require 'rails_helper'

RSpec.describe SubsectionsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.set_my_course
    @chapter = create(:chapter, course: @course)

    @section1 = create(:section, chapter: @chapter)
    @section2 = create(:section, chapter: @chapter)
    @section3 = create(:section, chapter: @chapter)

    # @subsection1 = create(:subsection, section: @section1, stars: 1)
    # @subsection2 = create(:subsection, section: @section1, stars: 2)
    # @subsection3 = create(:subsection, section: @section1, stars: 2)
  end

  describe "POST create" do
    it "should create a new subsection" do
      post :create, chapter_id: @chapter.id[0,8], section: {title: "new section"}
      expect(response).to redirect_to chapter_section_path(@chapter, assigns(:section))
    end
    it "should not create a invalid subsection" do
      post :create, chapter_id: @chapter.id[0,8], section: {title: ""}
      expect(response).to render_template('new')
    end
  end

  

end
