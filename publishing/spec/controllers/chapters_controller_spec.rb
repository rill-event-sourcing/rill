require 'rails_helper'

RSpec.describe ChaptersController, :type => :controller do

  before do
    @course = create(:course)
    session[:course_id] = @course.id
    controller.send :set_my_course
    @chapter = create(:chapter, course: @course)
    @chapter2 = create(:chapter, title: "second chapter", course: @course)
    @chapter3 = create(:chapter, title: "third chapter", course: @course)
  end

  describe 'GET index' do

    it 'should render the index template' do
      get :index
      expect(response).to render_template('index')
    end

    it "should select all the chapters for the current course" do
      get :index
      expect(assigns(:chapters)).to eq @course.chapters
    end

  end

  describe 'GET new' do

    before do
      get :new
    end

    it 'should render the new template' do
      expect(response).to render_template('new')
    end

    it "should create a new chapter" do
      expect(assigns(:chapter)).not_to eq nil
      expect(assigns(:chapter).new_record?).to eq true
    end
  end

  describe 'POST create' do

    it "should create a new chapter" do
      post :create, chapter: {id: @chapter.id, title: 'new title', description: 'my best description'}
      expect(assigns(:chapter).new_record?).to eq false
    end

    it "should redirect to the chapters path on successful save" do
      post :create, chapter: {id: @chapter.id, title: 'new title', description: 'my best description'}
      expect(response).to redirect_to chapters_path
    end

    it "should render the new template on unsuccessful save" do
      post :create, chapter: {id: @chapter.id}
      expect(response).to render_template('new')
    end

  end

###

  describe "PUT update" do

    it "should update the chapter" do
      put :update, id: @chapter.to_param, chapter: { title: 'new title', description: 'my best description'}
      expect(response).to redirect_to chapter_path @chapter
    end

    it "should not update the invalid chapter" do
      put :update, id: @chapter.to_param, chapter: {title: '', description: ''}
      expect(response).to render_template('edit')
    end
  end

  describe "POST destroy" do
    it "should trash the chapter and redirect" do
      post :destroy, id: @chapter.to_param
      expect(response).to redirect_to chapters_path
      expect(Chapter.trashed.first).to eq @chapter
    end
  end

  describe "POST activate" do
    it "should activate the chapter and redirect" do
      post :activate, id: @chapter.to_param
      expect(response).to redirect_to chapters_path
      expect(@chapter.active)
    end
  end

  describe "POST deactivate" do
    it "should deactivate the chapter and redirect" do
      post :deactivate, id: @chapter.to_param
      expect(response).to redirect_to chapters_path
      expect(!@chapter.active)
    end
  end

  describe "POST moveup" do

    it "should moveup the chapter and redirect" do
      expect(@chapter2.position).to eq 2
      post :moveup, id: @chapter2.to_param
      expect(assigns(:chapter)).to eq @chapter2
      expect(response).to redirect_to chapters_path
      @chapter2.reload
      expect(@chapter2.position).to eq 1
    end
  end

  describe "POST movedown" do
    it "should movedown the chapter and redirect" do
      expect(@chapter2.position).to eq 2
      post :movedown, id: @chapter2.to_param
      expect(assigns(:chapter)).to eq @chapter2
      expect(response).to redirect_to chapters_path
      @chapter2.reload
      expect(@chapter2.position).to eq 3
    end
  end

  describe "params filtering" do

    it "should throw when missing" do
      controller.params = {something: true}
      expect{controller.send(:chapter_params)}.to raise_error(ActionController::ParameterMissing)
    end

    it "should filter params" do
      controller.params = { 'chapter' => {title: 'my title', description: "my description", something_else: 'this should be filtered out'} }
      my_params = controller.send(:chapter_params)
      expect(my_params).to eq( {'title' => 'my title', 'description' => "my description"})
    end
  end
end
