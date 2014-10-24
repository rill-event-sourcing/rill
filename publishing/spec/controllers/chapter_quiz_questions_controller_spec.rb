require 'rails_helper'

RSpec.describe ChapterQuizQuestionsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @chapter = create(:chapter, course: @course)
    @quiz = create(:chapter_quiz, chapter: @chapter)
    @qs = create(:chapter_questions_set, chapter_quiz: @quiz)
    @q1 = create(:question, quizzable: @qs)
    @q2 = create(:question, quizzable: @qs)
    @q3 = create(:question, quizzable: @qs)
  end

  describe "GET preview" do
    it "should render the preview page" do
      get :preview, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param, id: @q1.to_param
      expect(response).to render_template('preview')
    end
  end

  describe 'POST create' do

    it "should create a new question" do
      post :create, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param
      expect(assigns(:question).new_record?).to eq false
    end

    it "should redirect to the entry quiz path on successful save" do
      post :create, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param
      @question = assigns(:question)
      expect(response).to redirect_to edit_chapter_chapter_quiz_chapter_questions_set_question_path(@chapter, @qs, @question)
    end
  end

  describe 'PUT update' do
    it "should call the input helper methods" do
      line_inputs = "my_line_input"
      multiple_choice_inputs = "my_multiple_choice"
      expect(controller).to receive(:set_line_inputs).once.with(@q1, line_inputs)
      expect(controller).to receive(:set_multiple_choice_inputs).once.with(@q1, multiple_choice_inputs)
      put :update, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param, id: @q1,
      id: @q1.to_param, question: {text: ""}, line_inputs: line_inputs,
      multiple_choice_inputs: multiple_choice_inputs, format: :json
    end
  end

  describe 'PUT destroy' do

    it "should destroy the question" do
      post :destroy, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param, id: @q1.to_param
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
    end

    it "should destroy the question more than once" do
      post :destroy, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param, id: @q1.to_param
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
      post :destroy, chapter_id: @chapter.to_param, chapter_questions_set_id: @qs.to_param, id: @q1.to_param
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
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

end
