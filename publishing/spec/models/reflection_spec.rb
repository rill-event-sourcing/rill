require 'rails_helper'

RSpec.describe Reflection, :type => :model do

  before do
    @reflection = create(:reflection)
    @reflection2 = create(:reflection)
  end

  it "should return an abbreviated uuid" do
    id = @reflection.id.to_s
    expect(@reflection.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Reflection.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Reflection.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Reflection.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple reflections by an abbreviated uuid" do
    uuid = Reflection.first.id
    Reflection.all.each do |reflection|
      reflection.update_attribute :id, uuid[0,8] + reflection.id[8,28]
    end
    expect{Reflection.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return its name correctly" do
    expect(@reflection.name).to eq "_REFLECTION_#{@reflection.position}_"
  end

  it "should increase the position of the section when generated" do
    @reflection2 = create(:reflection, position: 2)
    @reflection3 = create(:reflection)
    @reflection3.send(:set_position)
    expect(@reflection3.position).to eq (@reflection2.position+1)
  end
end
