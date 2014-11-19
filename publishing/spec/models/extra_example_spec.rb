require 'rails_helper'

RSpec.describe ExtraExample, :type => :model do

  before do
    @extra_example = create(:extra_example)
    @extra_example2 = create(:extra_example)
  end

  it "should return an abbreviated uuid" do
    id = @extra_example.id.to_s
    expect(@extra_example.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{ExtraExample.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{ExtraExample.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(ExtraExample.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple extra_examples by an abbreviated uuid" do
    uuid = ExtraExample.first.id
    ExtraExample.all.each do |extra_example|
      extra_example.update_attribute :id, uuid[0,8] + extra_example.id[8,28]
    end
    expect{ExtraExample.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return its name correctly" do
    expect(@extra_example.name).to eq "_EXTRA_EXAMPLE_#{@extra_example.position}_"
  end

  it "should increase the position of the section when generated" do
    @extra_example2 = create(:extra_example, position: 2)
    @extra_example3 = create(:extra_example)
    @extra_example3.send(:set_position)
    expect(@extra_example3.position).to eq (@extra_example2.position+1)
  end
end
