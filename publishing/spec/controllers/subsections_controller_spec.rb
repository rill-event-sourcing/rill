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

    @subsection1 = create(:subsection, section: @section1, stars: 1, position: 0)
    @subsection2 = create(:subsection, section: @section1, stars: 2, position: 0)
    @subsection3 = create(:subsection, section: @section1, stars: 2, position: 1)
  end

  describe "POST create" do
    it "should create a new subsection" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, stars: 2, position: 0
      @subsection = assigns(:subsection)
      expect(@subsection).not_to eq nil
      expect(!@subsection.new_record?).to eq true
      expect(assigns(:star)).to eq 2
      expect(assigns(:index)).to eq @subsection.id
      expect(response).to render_template('subsections/_edit')
    end

    it "should create a new subsection with position 0" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, stars: 2, position: 0
      @subsection = assigns(:subsection)
      expect(@section1.subsections.find_by_star(2)).to eq [@subsection, @subsection2, @subsection3]
    end

    it "should create a new subsection with position 1" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, stars: 2, position: 1
      @subsection = assigns(:subsection)
      expect(@section1.subsections.find_by_star(2)).to eq [@subsection2, @subsection, @subsection3]
    end

    it "should create a new subsection with position 2" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, stars: 2, position: 2
      @subsection = assigns(:subsection)
      expect(@section1.subsections.find_by_star(2)).to eq [@subsection2, @subsection3, @subsection]
    end

  end

  describe "POST destroy" do
    it "should destroy the section" do
      post :destroy,  chapter_id: @chapter.to_param, section_id: @section1.to_param, id: @subsection1.id
      expect(response.status).to eq(200)
    end
    it "should destroy the section more than once" do
      post :destroy,  chapter_id: @chapter.to_param, section_id: @section1.to_param, id: @subsection1.id
      expect(response.status).to eq(200)
      post :destroy,  chapter_id: @chapter.to_param, section_id: @section1.to_param, id: @subsection1.id
      expect(response.status).to eq(200)
    end
  end

end
