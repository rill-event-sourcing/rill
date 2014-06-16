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
      get :index, chapter_id: @chapter.to_param
      expect(response).to redirect_to @chapter
    end
  end

  describe "GET show" do
    it "should render the show page without subsections" do
      get :show, chapter_id: @chapter.to_param, id: @section1.to_param
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq({})
    end
    it "should render the show page with subsections" do
      @subsection1 = create(:subsection, section: @section1, stars: 1)
      @subsection2 = create(:subsection, section: @section1, stars: 2)
      @subsection3 = create(:subsection, section: @section1, stars: 2)

      get :show, chapter_id: @chapter.to_param, id: @section1.to_param
      expect(response).to render_template('show')
      expect(assigns(:all_subsections)).to eq(
        {
          1 => [@subsection1],
          2 => [@subsection2, @subsection3]
        })
    end
  end

  describe "GET new" do

    before do
      get :new, chapter_id: @chapter.to_param
    end

    it "should render the new page" do
      expect(response).to render_template('new')
    end

    it "should create a new section" do
      expect(assigns(:section)).not_to eq nil
      expect(assigns(:section).new_record?).to eq true
    end
  end

  describe "POST create" do
    it "should create a new section" do
      post :create, chapter_id: @chapter.to_param, section: {title: "new section"}
      expect(response).to redirect_to chapter_section_path(@chapter, assigns(:section))
    end
    it "should not create a invalid section" do
      post :create, chapter_id: @chapter.to_param, section: {title: ""}
      expect(response).to render_template('new')
    end
  end

  describe "PUT update" do
    it "should update the section" do
      put :update, chapter_id: @chapter.to_param, id: @section1.to_param, section: {title: "new section2"}
      expect(response).to redirect_to chapter_section_path(@chapter, assigns(:section))
    end
    it "should not update the invalid section" do
      put :update, chapter_id: @chapter.to_param, id: @section1.to_param, section: {title: ""}
      expect(response).to render_template('show')
    end
  end

  describe "GET preview" do
    it "should render a preview of the section" do
      @subsection1 = create(:subsection, section: @section1, stars: 1)
      @subsection2 = create(:subsection, section: @section1, stars: 2)
      @subsection3 = create(:subsection, section: @section1, stars: 2)
      get :preview, chapter_id: @chapter.to_param, id: @section1.to_param, star: 2
      expect(assigns(:star)).to eq '2'
      expect(assigns(:section)).to eq @section1
      expect(assigns(:subsections)).to eq [@subsection2, @subsection3]
      expect(response).to render_template('preview')
    end
  end

  describe "POST activate" do
    it "should activate the section and redirect" do
      @section1.update_attribute :active, false
      post :activate, chapter_id: @chapter.to_param, id: @section1.to_param
      expect(response).to redirect_to chapter_sections_path(@chapter)
      @section1.reload
      expect(@section1.active).to eq true
    end
  end

  describe "POST deactivate" do
    it "should deactivate the section and redirect" do
      @section1.update_attribute :active, true
      post :deactivate, chapter_id: @chapter.to_param, id: @section1.to_param
      expect(response).to redirect_to chapter_sections_path(@chapter)
      @section1.reload
      expect(@section1.active).to eq false
    end
  end

  describe "POST moveup" do
    it "should moveup the section and redirect" do
      expect(@section2.position).to eq 2
      post :moveup, chapter_id: @chapter.to_param, id: @section2.to_param
      expect(assigns(:section)).to eq @section2
      expect(response).to redirect_to chapter_sections_path(@chapter)
      @section2.reload
      expect(@section2.position).to eq 1
    end
  end

  describe "POST movedown" do
    it "should movedown the section and redirect" do
      expect(@section2.position).to eq 2
      post :movedown, chapter_id: @chapter.to_param, id: @section2.to_param
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




#
# context "as a container for subsections" do
#
#   before do
#     @oneone = create(:subsection, title: "oneone", position: 1, stars: 1, section: @section1, text: "oneone text" )
#     @onetwo = create(:subsection, title: "onetwo", position: 2, stars: 1, section: @section1, text: "onetwo text")
#     @twoone = create(:subsection, title: "twoone", position: 1, stars: 2, section: @section1, text: "twoone text")
#     @twotwo = create(:subsection, title: "twotwo", position: 2, stars: 2, section: @section1, text: "twotwo text")
#     @threeone = create(:subsection, title: "threeone", position: 1, stars: 3, section: @section1, text: "threeone text")
#     @threetwo = create(:subsection, title: "threetwo", position: 2, stars: 3, section: @section1, text: "threetwo text")
#   end
#
#   def update_first_subsection
#     subsection = @section1.subsections.find_by_star(1).first.as_full_json
#     subsection[:text] = "oneone modified text"
#
#     hashone = hashify [subsection.stringify,@onetwo.as_full_json.stringify]
#     hashtwo = hashify [@twoone.as_full_json.stringify,@twotwo.as_full_json.stringify]
#     hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]
#
#     input = hashify([hashone, hashtwo, hashthree], true)
#
#     @section1.subsections=input
#   end
#
#   it "should allow to update subsections" do
#     update_first_subsection
#     expect(@section1.subsections.find_by_star(1).first.as_full_json[:text]).to eq "oneone modified text"
#   end
#
#   it "should correctly reflect the time of last update" do
#     old_time = @section1.updated_at
#     update_first_subsection
#     expect(@section1.updated_at.to_f).to be > old_time.to_f
#   end
#
#   it "should respect the order of input subsections" do
#
#     first_subsection = @section1.subsections.find_by_star(2).first
#     last_subsection = @section1.subsections.find_by_star(2).last
#
#     hashone = hashify [@oneone.as_full_json.stringify,@onetwo.as_full_json.stringify]
#     hashtwo = hashify [last_subsection.as_full_json.stringify,first_subsection.as_full_json.stringify]
#     hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]
#
#     input = hashify([hashone, hashtwo, hashthree], true)
#
#     @section1.subsections=input
#
#     expect(@section1.subsections.find_by_star(2).first).to eq last_subsection
#     expect(@section1.subsections.find_by_star(2).last).to eq first_subsection
#   end
#
#
# end





end
