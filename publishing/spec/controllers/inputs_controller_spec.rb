require 'rails_helper'

RSpec.describe InputsController, :type => :controller do

  def set_inputs
    @input1 = create(:line_input, question: @question1)
    @input2 = create(:line_input, question: @question1)
    @input3 = create(:multiple_choice_input, question: @question1)
  end

  before do
    @course = create(:course)
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
    @question1 = create(:question, section: @section1)
  end


  describe "POST create" do
    before do
      set_inputs
    end

    it "should create a new line_input" do
      post :create,  question_id: @question1.to_param, input_type: 'line-input'
      @input = assigns(:input)
      expect(@input).not_to eq nil
      expect(!@input.new_record?).to eq true
      expect(response).to render_template('inputs/_edit')
    end

    it "should create a new multiple_choice_input" do
      post :create,  question_id: @question1.to_param, input_type: 'multiple-choice'
      @input = assigns(:input)
      expect(@input).not_to eq nil
      expect(!@input.new_record?).to eq true
      expect(response).to render_template('inputs/_edit')
    end

    it "should throw an error on unknown input" do
      expect{post :create,  question_id: @question1.to_param}.to raise_error('unknown input type')
    end
  end


  describe "POST destroy" do
    before do
      set_inputs
    end

    it "should destroy the input" do
      post :destroy,  question_id: @question1.to_param, id: @input1.to_param
      expect(response.status).to eq(200)
    end

    it "should destroy the choice more than once" do
      post :destroy,  question_id: @question1.to_param, id: @input1.to_param
      expect(response.status).to eq(200)
      post :destroy,  question_id: @question1.to_param, id: @input1.to_param
      expect(response.status).to eq(200)
    end
  end





end
