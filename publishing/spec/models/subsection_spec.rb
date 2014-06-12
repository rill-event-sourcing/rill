require 'rails_helper'

RSpec.describe Subsection, :type => :model do
  it {is_expected.to validate_presence_of :section }
  it {is_expected.to validate_presence_of :stars }

  before do
    @subsection1 = create(:subsection, title: "A", description: "A content", stars: 1)
    @subsection2 = create(:subsection, title: "B", description: "B content", stars: 2)
    @subsection3 = create(:subsection, title: "C", description: "C content", stars: 3)
  end

  it "should return the title when asked for a string" do
    @subsection = build(:subsection)
    expect(@subsection.to_s).to eq @subsection.title
  end

  it "should return an abbreviated uuid" do
    id = @subsection1.id.to_s
    expect(@subsection1.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Subsection.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Subsection.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Subsection.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple chapters by an abbreviated uuid" do
    uuid = Subsection.first.id
    Subsection.all.each do |subsection|
      subsection.update_attribute :id, uuid[0,8] + subsection.id[8,28]
    end
    expect{Subsection.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return a json object" do
    obj = {id: @subsection1.id, title: @subsection1.title, description: @subsection1.description}
    expect(@subsection1.as_json).to eq obj
  end

  it "should return a full json object" do
    obj = {
        id: @subsection1.id,
        position: @subsection1.position,
        stars: @subsection1.stars,
        title: @subsection1.title,
        description: @subsection1.description
      }
    expect(@subsection1.as_full_json).to eq obj
  end

end
