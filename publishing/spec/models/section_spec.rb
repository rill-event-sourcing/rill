require 'rails_helper'

RSpec.describe Section, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :chapter }

  before do
    create(:section, title: 'B', position: 2)
    create(:section, title: 'C', position: 3)
    @section = create(:section, title: 'A', position: 1)
  end

  it "should return title when asked for its string" do
    @section = build(:section)
    expect(@section.to_s).to eq @section.title
  end

  it "should list sections in order of order" do
    expect(Section.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed sections" do
    @section.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    expect(Section.trashed.first).to eq @section
  end

  it "should be activateable" do
    @section = build(:section)
    expect(@section.active).to eq false
    @section.activate
    expect(@section.active).to eq true
    @section.deactivate
    expect(@section.active).to eq false
  end
end
