class EntryQuizController < ApplicationController
  before_action :set_param_objects
  before_action :set_breadcrumb, except: [:create]

  def show
  end

  private

  def set_param_objects
    @course = Course.current
    @entry_quiz = @course.entry_quiz
  end

  def set_breadcrumb
    @crumbs = [{name: @course.name, url: root_path}]
    @crumbs << {name: "Entry Quiz", url: @entry_quiz}
  end

end
