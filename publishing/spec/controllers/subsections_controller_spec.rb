require 'rails_helper'

RSpec.describe SubsectionsController, :type => :controller do

  def set_subsections
    @subsection1 = create(:subsection, section: @section1, stars: 1, position: 0)
    @subsection2 = create(:subsection, section: @section1, stars: 2, position: 0)
    @subsection3 = create(:subsection, section: @section1, stars: 2, position: 1)
  end

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
    it "should render the index page without subsections" do
      get :index, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(response).to render_template('index')
      expect(assigns(:all_subsections)).to eq({})
    end

    it "should render the index page with subsections" do
      set_subsections
      get :index, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(response).to render_template('index')
      expect(assigns(:all_subsections)).to eq(
        {
          1 => [@subsection1],
          2 => [@subsection2, @subsection3]
        })
    end
  end


  describe "POST create" do
    before do
      set_subsections
    end

    it "should create a new subsection" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, stars: 2, position: 0
      @subsection = assigns(:subsection)
      expect(@subsection).not_to eq nil
      expect(!@subsection.new_record?).to eq true
      expect(assigns(:star)).to eq 2
      expect(assigns(:index)).to eq @subsection.id
      expect(response).to render_template('subsections/_edit')
    end

    it "should not create an invalid subsection" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(response.status).to eq(422)
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


  describe "GET preview" do
    before do
      set_subsections
    end

    it "should render a preview of the section" do
      get :preview, chapter_id: @chapter.to_param, section_id: @section1.to_param, star: 2
      expect(assigns(:star)).to eq '2'
      expect(assigns(:section)).to eq @section1
      expect(assigns(:subsections)).to eq [@subsection2, @subsection3]
      expect(response).to render_template('preview')
    end
  end


  describe "PUT update" do

    # before do
    #   @oneone = create(:subsection, title: "oneone", position: 1, stars: 1, section: @section1, text: "oneone text" )
    #   @onetwo = create(:subsection, title: "onetwo", position: 2, stars: 1, section: @section1, text: "onetwo text")
    #   @twoone = create(:subsection, title: "twoone", position: 1, stars: 2, section: @section1, text: "twoone text")
    #   @twotwo = create(:subsection, title: "twotwo", position: 2, stars: 2, section: @section1, text: "twotwo text")
    #   @threeone = create(:subsection, title: "threeone", position: 1, stars: 3, section: @section1, text: "threeone text")
    #   @threetwo = create(:subsection, title: "threetwo", position: 2, stars: 3, section: @section1, text: "threetwo text")
    # end

    # def update_first_subsection
    #   subsection = @section1.subsections.find_by_star(1).first.as_full_json
    #   subsection[:text] = "oneone modified text"
    #
    #   hashone = hashify [subsection.stringify,@onetwo.as_full_json.stringify]
    #   hashtwo = hashify [@twoone.as_full_json.stringify,@twotwo.as_full_json.stringify]
    #   hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]
    #   hashify([hashone, hashtwo, hashthree], true)
    # end
    #
    # it "should allow to update subsections" do
    #   input = update_first_subsection
    #   put :update, chapter_id: @chapter.to_param, id: @section1.to_param, section: {title: @section1.title, description: @section1.description}, subsections: input
    #   expect(@section1.subsections.find_by_star(1).first.as_full_json[:text]).to eq "oneone modified text"
    # end

  #   it "should correctly reflect the time of last update" do
  #       old_time = @section1.updated_at
  #       input = update_first_subsection
  #       put :update, chapter_id: @chapter.to_param, id: @section1.to_param, section: {title: @section1.title, description: @section1.description}, subsections: input
  #       #expect(@section1.updated_at.to_f).to be > old_time.to_f
  #     end
  #
  #     it "should respect the order of input subsections" do
  #
  #       first_subsection = @section1.subsections.find_by_star(2).first
  #       last_subsection = @section1.subsections.find_by_star(2).last
  #
  #       hashone = hashify [@oneone.as_full_json.stringify,@onetwo.as_full_json.stringify]
  #       hashtwo = hashify [last_subsection.as_full_json.stringify,first_subsection.as_full_json.stringify]
  #       hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]
  #
  #       input = hashify([hashone, hashtwo, hashthree], true)
  #       put :update, chapter_id: @chapter.to_param, id: @section1.to_param, section: {title: @section1.title, description: @section1.description}, subsections: input
  #
  #       expect(@section1.subsections.find_by_star(2).first).to eq last_subsection
  #       expect(@section1.subsections.find_by_star(2).last).to eq first_subsection
  #     end
  end

  describe "POST destroy" do
    before do
      set_subsections
    end

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
