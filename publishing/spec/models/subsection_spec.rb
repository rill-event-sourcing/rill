require 'rails_helper'

RSpec.describe Subsection, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :level }
  it {is_expected.to validate_presence_of :section }

  before do
    create(:subsection, title: 'B', position: 2)
    create(:subsection, title: 'C', position: 3)
    @subsection = create(:subsection, title: 'A', position: 1)
  end

  it "should return title when asked for its string" do
    @subsection = build(:subsection)
    expect(@subsection.to_s).to eq @subsection.title
  end

  it "should list subsections in the right order" do
    expect(Subsection.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed subsections" do
    @subsection.trash
    expect(Subsection.all.map(&:to_s)).to eq ['B', 'C']
    expect(Subsection.trashed.first).to eq @subsection
  end

  it "should be activateable" do
    @subsection = build(:subsection)
    expect(@subsection.active).to eq false
    @subsection.activate
    expect(@subsection.active).to eq true
    @subsection.deactivate
    expect(@subsection.active).to eq false
  end
end
