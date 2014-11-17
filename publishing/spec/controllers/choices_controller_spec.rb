require 'rails_helper'

RSpec.describe ChoicesController, :type => :controller do

  before do
    @course = create(:course)
    @chapter = create(:chapter, course: @course)
    @section1 = create(:section, chapter: @chapter)
    @question1 = create(:question, quizzable: @section1)
    @input1 = create(:multiple_choice_input, inputable: @question1)
    @choice1 = create(:choice, multiple_choice_input: @input1, position: 1)
    @choice2 = create(:choice, multiple_choice_input: @input1, position: 2)
    @choice3 = create(:choice, multiple_choice_input: @input1, position: 3)
  end


  describe "POST create" do
    it "should create a new choice" do
      post :create,  question_id: @question1.to_param, input_id: @input1.to_param
      @input = assigns(:input)
      expect(@input).not_to eq nil
      expect(!@input.new_record?).to eq true
      expect(response).to render_template('choices/_edit')
    end
  end

  describe "POST moveup" do
    it "should moveup the choices" do
      expect(@choice2.position).to eq 2
      post :moveup, input_id: @input1.to_param, id: @choice2.to_param, question_id: @question1
      expect(assigns(:choice)).to eq @choice2
      @choice2.reload
      expect(@choice2.position).to eq 1
    end
  end

  describe "POST movedown" do
    it "should movedown the choices" do
      expect(@choice2.position).to eq 2
      post :movedown, input_id: @input1.to_param, id: @choice2.to_param, question_id: @question1
      expect(assigns(:choice)).to eq @choice2
      @choice2.reload
      expect(@choice2.position).to eq 3
    end
  end


  describe "POST destroy" do

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
