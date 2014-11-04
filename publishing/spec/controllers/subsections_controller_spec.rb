require 'rails_helper'

RSpec.describe SubsectionsController, :type => :controller do

  def set_subsections
    @subsection1 = create(:subsection, section: @section1, position: 2)
    @subsection2 = create(:subsection, section: @section1, position: 0)
    @subsection3 = create(:subsection, section: @section1, position: 1)
  end

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
    @section2 = create(:section, chapter: @chapter)
    @section3 = create(:section, chapter: @chapter)
  end


  describe "GET index" do
    it "should render the index page without subsections" do
      get :index, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(response).to render_template('index')
      expect(@section1.subsections.to_a).to eq []
    end

    it "should render the index page with subsections" do
      set_subsections
      get :index, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(response).to render_template('index')
      expect(@section1.subsections.to_a).to eq [@subsection2, @subsection3, @subsection1]
    end
  end


  describe "POST create" do
    before do
      set_subsections
    end

    it "should create a new subsection" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, position: 0
      @subsection = assigns(:subsection)
      expect(@subsection).not_to eq nil
      expect(!@subsection.new_record?).to eq true
      expect(assigns(:index)).to eq @subsection.id
      expect(response).to render_template('subsections/_edit')
    end

    it "should create a new subsection with position 0" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, position: 0
      @subsection = assigns(:subsection)
      expect(@section1.subsections).to eq [@subsection, @subsection2, @subsection3, @subsection1]
    end

    it "should create a new subsection with position 1" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, position: 1
      @subsection = assigns(:subsection)
      expect(@section1.subsections).to eq [@subsection2, @subsection, @subsection3, @subsection1]
    end

    it "should create a new subsection with position 2" do
      post :create, chapter_id: @chapter.to_param, section_id: @section1.to_param, position: 3
      @subsection = assigns(:subsection)
      expect(@section1.subsections).to eq [@subsection2, @subsection3, @subsection1, @subsection]
    end
  end


  describe "GET preview" do
    before do
      set_subsections
    end

    it "should render a preview of the section" do
      get :preview, chapter_id: @chapter.to_param, section_id: @section1.to_param
      expect(assigns(:section)).to eq @section1
      expect(@section1.subsections).to eq [@subsection2, @subsection3, @subsection1]
      expect(response).to render_template('preview')
    end
  end


  describe "POST save" do

    before do
      @subsection1 = create(:subsection, title: "one", position: 0, section: @section1, text: "one text" )
      @subsection2 = create(:subsection, title: "two", position: 1, section: @section1, text: "two text")
    end

    def update_first_subsection
      subsection1 = @section1.subsections.first#.as_full_json
      subsection1.text = "one modified text"
      subsection2 = @section1.subsections.last#.as_full_json
      hashify([subsection1, subsection2])
    end

    it "should allow to update subsections" do
      input = update_first_subsection
      post :save, chapter_id: @chapter.to_param, section_id: @section1.to_param, subsections: input, format: :json
      expect(@section1.subsections.first.text).to eq "one modified text"
    end

    it "should respect the order of input subsections" do
      first_subsection = @section1.subsections.first
      last_subsection = @section1.subsections.last
      first_subsection.position = 1
      last_subsection.position = 0

      subsection_hash = hashify([last_subsection, first_subsection])

      post :save, chapter_id: @chapter.to_param, section_id: @section1.to_param, subsections: subsection_hash, format: :json

      expect(@section1.subsections.first).to eq last_subsection
      expect(@section1.subsections.last).to eq first_subsection
    end
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

  describe "setting inputs" do
    it "should correctly set line inputs" do
      @section = create(:section)
      @input = create(:line_input, inputable: @section)
      @answer = create(:answer, line_input: @input, value: "ok")
      new_value = "I changed this!"
      line_inputs_hash = {
        "#{@input.id}"=> {
          prefix: "new_pre",
          suffix: "after_post",
          width: 10,
          answers: {
            "#{@answer.id}"=>{
              value: new_value
            }
          }
        }
      }
      controller.send(:set_line_inputs, @section, line_inputs_hash)
      @input.reload
      expect(@input.prefix).to eq "new_pre"
      expect(@input.suffix).to eq "after_post"
      expect(@input.width).to eq 10
      @answer.reload
      expect(@answer.value).to eq new_value
    end
  end

end
