require 'rails_helper'

RSpec.describe SectionsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.set_my_course
    @chapter = create(:chapter, course: @course)

    @section1 = create(:section, chapter: @chapter)
    @section2 = create(:section, chapter: @chapter)
    @section3 = create(:section, chapter: @chapter)
  end

  describe "GET index" do
    it "should redirect to the chapter" do
      get :index, chapter_id: @chapter.id[0,8]
      expect(response).to redirect_to @chapter
    end
  end

  describe "GET show" do
    it "should render the show page without subsections" do
      get :show, chapter_id: @chapter.id[0,8], id: @section1.id[0,8]
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq({})
    end
    it "should render the show page with subsections" do
      @subsection1 = create(:subsection, section: @section1, stars: 1)
      @subsection2 = create(:subsection, section: @section1, stars: 2)
      @subsection3 = create(:subsection, section: @section1, stars: 2)

      get :show, chapter_id: @chapter.id[0,8], id: @section1.id[0,8]
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq(
        {
          1 => [@subsection1],
          2 => [@subsection2, @subsection3]
        })
    end
  end

  describe "GET new" do
    it "should render the new page" do
      get :new, chapter_id: @chapter.id[0,8]
      expect(response).to render_template('new')
      expect(assigns(:section)).not_to eq nil
      expect(assigns(:section).new_record?)
    end
  end

  describe "POST create" do
    it "should create a new section" do
      post :create, chapter_id: @chapter.id[0,8], section: {title: "new section"}
      expect(response).to redirect_to chapter_section_path(@chapter, assigns(:section))
    end
    it "should not create a invalid section" do
      post :create, chapter_id: @chapter.id[0,8], section: {title: ""}
      expect(response).to render_template('new')
    end
  end

  describe "PUT update" do
    it "should update the section" do
      put :update, chapter_id: @chapter.id[0,8], id: @section1.id[0,8], section: {title: "new section2"}
      expect(response).to redirect_to chapter_section_path(@chapter, assigns(:section))
    end
    it "should not update the invalid section" do
      put :update, chapter_id: @chapter.id[0,8], id: @section1.id[0,8], section: {title: ""}
      expect(response).to render_template('show')
    end
  end

  describe "POST activate" do
    it "should activate the section and redirect" do
      post :activate, chapter_id: @chapter.id[0,8], id: @section1.id[0,8]
      expect(response).to redirect_to chapter_sections_path(@chapter)
      expect(@section1.active)
    end
  end

  describe "POST deactivate" do
    it "should deactivate the section and redirect" do
      post :deactivate, chapter_id: @chapter.id[0,8], id: @section1.id[0,8]
      expect(response).to redirect_to chapter_sections_path(@chapter)
      expect(!@section1.active)
    end
  end

  describe "POST moveup" do
    it "should moveup the section and redirect" do
      expect(@section2.position).to eq 2
      post :moveup, chapter_id: @chapter.id[0,8], id: @section2.id[0,8]
      expect(assigns(:section)).to eq @section2
      expect(response).to redirect_to chapter_sections_path(@chapter)
      @section2.reload
      expect(@section2.position).to eq 1
    end
  end

  describe "POST movedown" do
    it "should movedown the section and redirect" do
      expect(@section2.position).to eq 2
      post :movedown, chapter_id: @chapter.id[0,8], id: @section2.id[0,8]
      expect(assigns(:section)).to eq @section2
      expect(response).to redirect_to chapter_sections_path(@chapter)
      @section2.reload
      expect(@section2.position).to eq 3
    end
  end


  describe "params filtering" do
    it "should throw when missing" do
      controller.params = {something: true}
      expect{controller.send(:section_params)}.to raise_error(ActionController::ParameterMissing)
    end
    it "should filter params (turned off for now)" do
      controller.params = { 'section' => {title: 'my title', description: "my description"} }
      my_params = controller.send(:section_params)
      expect(my_params).to eq( {'title' => 'my title', 'description' => "my description"})
    end
  end

end
