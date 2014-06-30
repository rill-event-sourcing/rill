require 'rails_helper'

RSpec.describe Input, :type => :model do

  it {is_expected.to validate_presence_of :question }

  before do
    @line_input = create(:line_input)
    @multiple_choice_input = create(:multiple_choice_input)
  end

  it "should return an abbreviated uuid" do
    id = @line_input.id.to_s
    expect(@line_input.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Input.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Input.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Input.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple inputs by an abbreviated uuid" do
    uuid = Input.first.id
    Input.all.each do |input|
      input.update_attribute :id, uuid[0,8] + input.id[8,28]
    end
    expect{Input.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should recognize whether it is a line input" do
    expect(@line_input.line_input?).to be true
    expect(@multiple_choice_input.line_input?).to be false
  end

  it "should recognize whether it is a multiple choice input" do
    expect(@line_input.multiple_choice_input?).to be false
    expect(@multiple_choice_input.multiple_choice_input?).to be true
  end

  it "should return its name correctly" do
    expect(@line_input.name).to eq "_INPUT_#{@line_input.position}_"
  end

  it "should increase the position of the question when generated" do
    @line_input2 = create(:line_input, position: 2)
    @line_input3 = create(:line_input)
    @line_input3.send(:set_position)
    expect(@line_input3.position).to eq (@line_input2.position+1)
  end
end
