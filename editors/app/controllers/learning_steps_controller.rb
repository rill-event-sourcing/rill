class LearningStepsController < ApplicationController
  before_action :set_learning_step, only: [:show, :edit, :update, :destroy]

  # GET /learning_steps
  # GET /learning_steps.json
  def index
    @learning_steps = LearningStep.all
  end

  # GET /learning_steps/1
  # GET /learning_steps/1.json
  def show
  end

  # GET /learning_steps/new
  def new
    @learning_step = LearningStep.new
  end

  # GET /learning_steps/1/edit
  def edit
  end

  # POST /learning_steps
  # POST /learning_steps.json
  def create
    @learning_step = LearningStep.new(learning_step_params)

    respond_to do |format|
      if @learning_step.save
        format.html { redirect_to @learning_step, notice: 'Learning step was successfully created.' }
        format.json { render :show, status: :created, location: @learning_step }
      else
        format.html { render :new }
        format.json { render json: @learning_step.errors, status: :unprocessable_entity }
      end
    end
  end

  # PATCH/PUT /learning_steps/1
  # PATCH/PUT /learning_steps/1.json
  def update
    respond_to do |format|
      if @learning_step.update(learning_step_params)
        format.html { redirect_to @learning_step, notice: 'Learning step was successfully updated.' }
        format.json { render :show, status: :ok, location: @learning_step }
      else
        format.html { render :edit }
        format.json { render json: @learning_step.errors, status: :unprocessable_entity }
      end
    end
  end

  # DELETE /learning_steps/1
  # DELETE /learning_steps/1.json
  def destroy
    @learning_step.destroy
    respond_to do |format|
      format.html { redirect_to learning_steps_url, notice: 'Learning step was successfully destroyed.' }
      format.json { head :no_content }
    end
  end

  private
    # Use callbacks to share common setup or constraints between actions.
    def set_learning_step
      @learning_step = LearningStep.find(params[:id])
    end

    # Never trust parameters from the scary internet, only allow the white list through.
    def learning_step_params
      params.require(:learning_step).permit(:name, :description, :chapter_id)
    end
end
