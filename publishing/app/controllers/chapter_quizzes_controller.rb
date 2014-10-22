class ChapterQuizzesController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb, except: [:create]

  def show
  end

  # def new
  # end

  # def create
  #   @entry_quiz = @course.build_entry_quiz(entry_quiz_params)
  #   if @entry_quiz.save
  #     redirect_to entry_quiz_path, notice: "Entry quiz was created successfully"
  #   else
  #     render :new
  #   end
  # end

  # def edit
  # end

  # def update
  #   if @entry_quiz.update(entry_quiz_params)
  #     redirect_to entry_quiz_path, notice: "Entry Quiz successfully updated."
  #   else
  #     render :edit
  #   end
  # end

  private

  def set_param_objects
    @course = Course.current
    @chapter = @course.chapters.find_by_uuid(params[:chapter_id])
    @chapter_quiz = @chapter.chapter_quiz || @chapter.create_chapter_quiz
  end

  def set_breadcrumb
    #@crumbs = [{name: @chapter.name, url: root_path}]
    #@crumbs << {name: "Chapter Quiz", url: entry_quiz_path}
  end

  def entry_quiz_params
    params.require(:chapter_quiz).permit()
  end

end
