require 'rails_helper'

RSpec.describe EntryQuizQuestionsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @eq = create(:entry_quiz, course: @course)
    @question1 = create(:question, quizzable: @eq)
    @question2 = create(:question, quizzable: @eq)
    @question3 = create(:question, quizzable: @eq)
  end

  describe 'GET index' do
    it "should render the index page" do
      get :index, entry_quiz_id: @eq.to_param
      expect(response).to render_template('index')
    end

    it "should correctly set the params" do
      get :index, entry_quiz_id: @eq.to_param
      expect(assigns(:course)).to eq @course
      expect(assigns(:entry_quiz)).to eq @eq
    end
  end

  describe 'POST create' do

    it "should create a new question" do
      post :create, entry_quiz_id: @eq.to_param
      expect(assigns(:question).new_record?).to eq false
    end

    it "should redirect to the entry quiz path on successful save" do
      post :create, entry_quiz_id: @eq.to_param
      @question = assigns(:question)
      expect(response).to redirect_to edit_entry_quiz_question_path(@question)
    end
  end


  describe 'PUT update' do
    it "should call the input helper methods" do
      line_inputs = "my_line_input"
      multiple_choice_inputs = "my_multiple_choice"
      expect(controller).to receive(:set_line_inputs).once.with(@question1, line_inputs)
      expect(controller).to receive(:set_multiple_choice_inputs).once.with(@question1, multiple_choice_inputs)
      put :update, entry_quiz_id: @eq.to_param,
                  id: @question1.to_param, question: {text: ""}, line_inputs: line_inputs,
                  multiple_choice_inputs: multiple_choice_inputs, format: :json
    end
  end

  describe 'PUT destroy' do

    it "should destroy the question" do
      post :destroy, chapter_id: @chapter.to_param, section_id: @section.to_param, id: @question1.to_param
      expect(response).to redirect_to entry_quiz_questions_path
    end

    it "should destroy the question more than once" do
      post :destroy, entry_quiz_id: @eq.to_param, id: @question1.to_param
      expect(response).to redirect_to entry_quiz_questions_path
      post :destroy, entry_quiz_id: @eq.to_param, id: @question1.to_param
      expect(response).to redirect_to entry_quiz_questions_path
    end
  end

  describe "POST activate" do
    it "should activate the question and redirect" do
      post :activate, entry_quiz_id: @eq.to_param, id: @question1.to_param
      expect(response).to redirect_to entry_quiz_questions_path
      expect(@question1.active)
    end
  end

  describe "POST deactivate" do
    it "should deactivate the question and redirect" do
      post :deactivate, entry_quiz_id: @eq.to_param, id: @question1.to_param
      expect(response).to redirect_to entry_quiz_questions_path
      expect(!@question1.active)
    end
  end

  describe "params filtering" do

    it "should throw when missing" do
      controller.params = {something: true}
      expect{controller.send(:question_params)}.to raise_error(ActionController::ParameterMissing)
    end

  end

  describe "setting inputs" do
    it "should correctly set line inputs" do
      @question = create(:question)
      @input = create(:line_input, inputable: @question)
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
      controller.send(:set_line_inputs, @question, line_inputs_hash)
      @input.reload
      expect(@input.prefix).to eq "new_pre"
      expect(@input.suffix).to eq "after_post"
      expect(@input.width).to eq 10
      @answer.reload
      expect(@answer.value).to eq new_value
    end

    it "should correctly set multiple choice inputs" do
      @question = create(:question)
      @input = create(:multiple_choice_input, inputable: @question)
      @choice = create(:choice, multiple_choice_input: @input, value: "ok")
      new_value = "I changed this!"
      multiple_choice_input_hash = {
        "#{@input.id}"=> {
          choices: {
            "#{@choice.id}"=>{
              value: new_value
            }
          }
        }
      }
      controller.send(:set_multiple_choice_inputs, @question, multiple_choice_input_hash)
      @choice.reload
      expect(@choice.value).to eq new_value
    end
  end

  describe "POST moveup" do

    it "should moveup the question and redirect" do
      expect(@question2.position).to eq 2
      post :moveup, id: @question2.to_param
      expect(assigns(:question)).to eq @question2
      expect(response).to redirect_to entry_quiz_questions_path
      @question2.reload
      expect(@question2.position).to eq 1
    end
  end

  describe "POST movedown" do
    it "should movedown the question and redirect" do
      expect(@question2.position).to eq 2
      post :movedown, id: @question2.to_param
      expect(assigns(:question)).to eq @question2
      expect(response).to redirect_to entry_quiz_questions_path
      @question2.reload
      expect(@question2.position).to eq 3
    end
  end


end
