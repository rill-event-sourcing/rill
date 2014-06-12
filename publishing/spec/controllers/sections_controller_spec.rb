require 'rails_helper'

RSpec.describe SectionsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.set_my_course
    @chapter = create(:chapter, course: @course)
    @section = create(:section, chapter: @chapter)
  end

  describe "GET index" do
    it "should redirect to the chapter" do
      get :index, chapter_id: @chapter.id[0,8]
      expect(response).to redirect_to @chapter
    end
  end

  describe "GET show" do
    it "should render the show page without subsections" do
      get :show, chapter_id: @chapter.id[0,8], id: @section.id[0,8]
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq({})
    end
    it "should render the show page with subsections" do
      @subsection1 = create(:subsection, section: @section, stars: 1)
      @subsection2 = create(:subsection, section: @section, stars: 2)
      @subsection3 = create(:subsection, section: @section, stars: 2)

      get :show, chapter_id: @chapter.id[0,8], id: @section.id[0,8]
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq(
        {
          1 => [@subsection1],
          2 => [@subsection2, @subsection3]
        })
    end
  end


end
