require 'rails_helper'

RSpec.describe EntryQuizzesController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @eq = create(:entry_quiz, course: @course)
    @new_instructions = "Do this and that"
    @new_feedback = "Good job"
    @question1 = create(:question, quizzable: @eq)
    @question2 = create(:question, quizzable: @eq)
    @question3 = create(:question, quizzable: @eq)
  end

  describe 'GET show' do

    it 'should render the index template' do
      get :show
      expect(response).to render_template('show')
    end

    it "should select the entry quiz of the current course" do
      get :show
      expect(assigns(:entry_quiz)).to eq @eq
    end

  end

  describe 'GET new' do

    before do
      get :new
    end

    it 'should render the new template' do
      expect(response).to render_template('new')
    end

    it "should create a new record for entry_quiz" do
      expect(assigns(:entry_quiz)).not_to eq nil
    end
  end

  describe 'POST create' do

    it "should create an entry quiz" do
      post :create, entry_quiz: {instructions: @new_instructions, feedback: @new_feedback}
      expect(assigns(:entry_quiz).new_record?).to eq false
    end

    it "should redirect to the entry quiz page on successful save" do
      post :create, entry_quiz: {instructions: @new_instructions, feedback: @new_feedback}
      expect(response).to redirect_to entry_quiz_path
      expect(assigns(:entry_quiz).instructions).to eq @new_instructions
      expect(assigns(:entry_quiz).feedback).to eq @new_feedback
    end

  end

  # ###

  describe "PUT update" do

    it "should update the chapter" do
      put :update, id: @entry_quiz.to_param, entry_quiz: {instructions: @new_instructions, feedback: @new_feedback}
      expect(response).to redirect_to entry_quiz_path
    end
  end

  # describe "params filtering" do

  #   it "should throw when missing" do
  #     controller.params = {something: true}
  #     expect{controller.send(:chapter_params)}.to raise_error(ActionController::ParameterMissing)
  #   end

  #   it "should filter params" do
  #     controller.params = { 'chapter' => {title: 'my title', description: "my description", something_else: 'this should be filtered out'} }
  #     my_params = controller.send(:chapter_params)
  #     expect(my_params).to eq( {'title' => 'my title', 'description' => "my description"})
  #   end
  # end



end
