require 'rails_helper'

RSpec.describe Choice, type: :model do

  it {is_expected.to belong_to :multiple_choice_input}

  before do
    @mc = create(:multiple_choice_input)
    @choice = create(:choice, position: 1, multiple_choice_input: @mc)
    @choice2 = create(:choice, position: 2, multiple_choice_input: @mc)
  end

  it "should return an abbreviated uuid" do
    id = @choice.id.to_s
    expect(@choice.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Choice.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Choice.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Choice.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should correctly reorder choices" do
    @choice.move_lower
    expect(@choice.position).to eq 2
    @choice2.reload
    expect(@choice2.position).to eq 1
    @choice.move_higher
    expect(@choice.position).to eq 1
    @choice2.reload
    expect(@choice2.position).to eq 2
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple choices by an abbreviated uuid" do
    uuid = Choice.first.id
    Choice.all.each do |choice|
      choice.update_attribute :id, uuid[0,8] + choice.id[8,28]
    end
    expect{Choice.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

end
