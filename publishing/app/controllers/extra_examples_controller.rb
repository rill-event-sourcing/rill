class ExtraExamplesController < ApplicationController
  before_action :set_param_objects

  def create
    @extra_example = @section.extra_examples.build
    if @extra_example.save
      render partial: 'edit', locals: {extra_example: @extra_example}
    else
      return head :unprocessable_entity
    end
  end

  def destroy
    @extra_example.destroy if @extra_example
    render json: { status: :ok }
  end

  private

  def set_param_objects
    @section = Section.find_by_uuid(params[:section_id])
    @extra_example = @section.extra_examples.find_by_uuid(params[:id], false) if params[:id]
  end

end
