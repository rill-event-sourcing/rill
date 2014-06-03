class CoursesController < ApplicationController
  before_action :set_course, only: [:show, :edit, :update, :destroy]

  def select
    if params[:course] && params[:course][:id].to_i > 0
      @course = Course.find(params[:course][:id])
      Course.current = @course
      session[:course_id] = @course.id
    else
      Course.current = nil
      session[:course_id] = nil
    end
    redirect_to root_path
  end

  def index
    @courses = Course.all
  end

  def show
  end

  def new
    @course = Course.new
  end

  def edit
  end

  def create
    @course = Course.new(course_params)
    if @course.save
      redirect_to courses_path, notice: 'Course was successfully created.'
    else
      render :new
    end
  end

  def update
    if @course.update(course_params)
      redirect_to @course, notice: 'Course was successfully updated.'
    else
      render :edit
    end
  end

  def destroy
    @course.trash
    redirect_to courses_url, notice: 'Course was successfully destroyed.'
  end

private

  def set_course
    @course = Course.find(params[:id])
  end

  def course_params
    params.require(:course).permit(:name, :description)
  end

end
