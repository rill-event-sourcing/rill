require 'rails_helper'

RSpec.describe ReflectionsController, :type => :controller do

  def set_reflections
    @reflection1 = create(:reflection, section: @section1)
    @reflection2 = create(:reflection, section: @section1)
  end

  before do
    @course = create(:course)
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
  end


  describe "POST create" do
    before do
      set_reflections
    end
    it "should create a new reflection" do
      post :create, section_id: @section1.to_param
      @reflection = assigns(:reflection)
      expect(@reflection).not_to eq nil
      expect(!@reflection.new_record?).to eq true
      expect(response).to render_template('reflections/_edit')
    end
  end


  describe "POST destroy" do
    before do
      set_reflections
    end

    it "should destroy the reflection" do
      post :destroy, section_id: @section1.to_param, id: @reflection1.to_param
      expect(response.status).to eq(200)
    end

    it "should destroy the choice more than once" do
      post :destroy, section_id: @section1.to_param, id: @reflection1.to_param
      expect(response.status).to eq(200)
      post :destroy, section_id: @section1.to_param, id: @reflection1.to_param
      expect(response.status).to eq(200)
    end
  end



end
