require 'rails_helper'

RSpec.describe ChoicesController, :type => :controller do

  def set_choices
    @choice1 = create(:choice, multiple_choice_input: @input1)
    @choice2 = create(:choice, multiple_choice_input: @input1)
    @choice3 = create(:choice, multiple_choice_input: @input1)
  end

  before do
    @course = create(:course)
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
    @question1 = create(:question, questionable: @section1)
    @input1 = create(:multiple_choice_input, question: @question1)
  end


  describe "POST create" do
    before do
      set_choices
    end

    it "should create a new subsection" do
      post :create,  question_id: @question1.to_param, input_id: @input1.to_param
      @input = assigns(:input)
      expect(@input).not_to eq nil
      expect(!@input.new_record?).to eq true
      expect(response).to render_template('choices/_edit')
    end
  end


  describe "POST destroy" do
    before do
      set_choices
    end

    it "should destroy the choice" do
      post :destroy,  question_id: @question1.to_param, input_id: @input1.to_param, id: @choice1.to_param
      expect(response.status).to eq(200)
    end

    it "should destroy the choice more than once" do
      post :destroy,  question_id: @question1.to_param, input_id: @input1.to_param, id: @choice1.to_param
      expect(response.status).to eq(200)
      post :destroy,  question_id: @question1.to_param, input_id: @input1.to_param, id: @choice1.to_param
      expect(response.status).to eq(200)
    end
  end

end
