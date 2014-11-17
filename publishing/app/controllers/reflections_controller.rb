class ReflectionsController < ApplicationController
  before_action :set_param_objects

  def create
    @reflection = @section.reflections.build
    if @reflection.save
      render partial: 'edit', locals: {reflection: @reflection}
    else
      return head :unprocessable_entity
    end
  end

  def destroy
    @reflection.destroy if @reflection
    render json: { status: :ok }
  end

  private

  def set_param_objects
    @section = Section.find_by_uuid(params[:section_id])
    @reflection = @section.reflections.find_by_uuid(params[:id], false) if params[:id]
  end

end
