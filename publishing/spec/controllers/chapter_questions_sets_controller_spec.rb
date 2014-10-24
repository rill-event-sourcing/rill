require 'rails_helper'

RSpec.describe ChapterQuestionsSetsController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @chapter = create(:chapter, course: @course)
    @quiz = create(:chapter_quiz, chapter: @chapter)
    @qs = create(:chapter_questions_set, title: "one", chapter_quiz: @quiz)
    @qs2 = create(:chapter_questions_set, title: "two", chapter_quiz: @quiz)
  end

  describe "GET new" do
    before do
      get :new, chapter_id: @chapter.to_param
    end

    it 'should render the new template' do
      expect(response).to render_template('new')
    end

    it "should create a question set" do
      expect(assigns(:chapter_questions_set)).not_to eq nil
      expect(assigns(:chapter_questions_set).new_record?).to eq true
    end

  end

  describe 'POST create' do

    it "should create a new question set" do
      post :create, chapter_id: @chapter, chapter_questions_set: {id: @qs.id, title: 'Another'}
      expect(assigns(:chapter_questions_set).new_record?).to eq false
    end

    it "should redirect to the chapter quiz path on successful save" do
      post :create, chapter_id: @chapter, chapter_questions_set: {id: @qs.id, title: 'Another'}
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
    end

  end

  describe "PUT update" do

    it "should update the question set" do
      put :update, chapter_id: @chapter, id: @qs.to_param, chapter_questions_set: {id: @qs.to_param, title: "Another"}
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
    end

  end

  describe "POST destroy" do
    it "should trash the chapter question set and redirect" do
      post :destroy, chapter_id: @chapter, id: @qs.to_param
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
      expect(ChapterQuestionsSet.find_by_uuid(@qs.to_param, false)).to eq nil
    end
  end

  describe "POST moveup" do

    it "should moveup the question set and redirect" do
      expect(@qs2.position).to eq 2
      post :moveup, chapter_id: @chapter, id: @qs2
      expect(assigns(:chapter_questions_set)).to eq @qs2
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
      @qs2.reload
      expect(@qs2.position).to eq 1
    end
  end

  describe "POST movedown" do

    it "should movedown the question set and redirect" do
      qs3 = create(:chapter_questions_set, title: "three", chapter_quiz: @quiz)
      qs4 = create(:chapter_questions_set, title: "four", chapter_quiz: @quiz)
      expect(qs3.position).to eq 3
      post :movedown, chapter_id: @chapter, id: qs3.to_param
      expect(assigns(:chapter_questions_set)).to eq qs3
      expect(response).to redirect_to chapter_chapter_quiz_path(@chapter)
      qs3.reload
      expect(qs3.position).to eq 4
    end
  end

  describe "params filtering" do

    it "should throw when missing" do
      controller.params = {something: true}
      expect{controller.send(:questions_set_params)}.to raise_error(ActionController::ParameterMissing)
    end

    it "should filter params" do
      controller.params = { 'chapter_questions_set' => {title: 'my title', something_else: 'this should be filtered out'} }
      my_params = controller.send(:questions_set_params)
      expect(my_params).to eq( {'title' => 'my title'})
    end
  end

end
