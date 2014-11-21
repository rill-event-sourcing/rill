class HomeController < ApplicationController

  before_action :set_redirect_cookie, only: [:index]
  before_action :set_breadcrumb

  def health_check
    # test being up and sane
    if Course.first # getting a Course from the database is an indication
      render json: { status: 'up' }
    else
      return head :service_unavailable
    end
  end

  def error_check
    # Raygun.track_exception()
    throw "error check form home_controller"
  end

  def index
  end

  private

  def set_breadcrumb
    @crumbs = [{name: Course.current.name, url: root_path}] if Course.current
  end
end
