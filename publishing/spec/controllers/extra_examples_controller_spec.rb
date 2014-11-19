require 'rails_helper'

RSpec.describe ExtraExamplesController, :type => :controller do

  def set_extra_examples
    @extra_example1 = create(:extra_example, section: @section1)
    @extra_example2 = create(:extra_example, section: @section1)
  end

  before do
    @course = create(:course)
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
  end


  describe "POST create" do
    before do
      set_extra_examples
    end
    it "should create a new extra_example" do
      post :create, section_id: @section1.to_param
      @extra_example = assigns(:extra_example)
      expect(@extra_example).not_to eq nil
      expect(!@extra_example.new_record?).to eq true
      expect(response).to render_template('extra_examples/_edit')
    end
  end


  describe "POST destroy" do
    before do
      set_extra_examples
    end

    it "should destroy the extra_example" do
      post :destroy, section_id: @section1.to_param, id: @extra_example1.to_param
      expect(response.status).to eq(200)
    end

    it "should destroy the choice more than once" do
      post :destroy, section_id: @section1.to_param, id: @extra_example1.to_param
      expect(response.status).to eq(200)
      post :destroy, section_id: @section1.to_param, id: @extra_example1.to_param
      expect(response.status).to eq(200)
    end
  end



end
