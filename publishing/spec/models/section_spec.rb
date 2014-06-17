require 'rails_helper'

RSpec.describe Section, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :chapter }
  it {is_expected.to have_many :subsections}

  before do
    @section1 = create(:section, title: 'B', position: 2)
    @section2 = create(:section, title: 'C', position: 3)
    @section3 = create(:section, title: 'A', position: 1)
    @subsection1 = create(:subsection, title: "A", text: "A content", stars: 1, section: @section1)
    @subsection2 = create(:subsection, title: "B", text: "A content", stars: 2, section: @section1)
    @subsection3 = create(:subsection, title: "C", text: "A content", stars: 3, section: @section1)
  end


  it "should list sections in the right order" do
    expect(Section.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed sections" do
    @section3.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    expect(Section.trashed.first).to eq @section3
  end

  end

  it "should list recovered sections" do
    @section3.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    @section3.recover
    expect(Section.all.map(&:to_s)).to eq ['A','B', 'C']
  end

  it "should be activateable" do
    @section = build(:section)
    expect(@section.active).to eq false
    @section.activate
    expect(@section.active).to eq true
    @section.deactivate
    expect(@section.active).to eq false
  end

  it "should return an abbreviated uuid" do
    id = @section1.id.to_s
    expect(@section1.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Section.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Section.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Section.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple Sections by an abbreviated uuid" do
    uuid = Section.first.id
    Section.all.each do |section|
      section.update_attribute :id, uuid[0,8] + section.id[8,28]
    end
    expect{Section.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return a json object" do
    subsections_by_level = {"1_star" => [], "2_star" => [], "3_star" => []}
    @section1.subsections.each do |subsection|
      subsections_by_level["#{subsection.stars}_star"] << subsection.as_json
    end
    obj = {id: @section1.id, title: @section1.title, subsections_by_level: subsections_by_level}
    expect(@section1.as_json).to eq obj
  end

  it "should return a full json object" do
    obj = {
      id: @section1.id,
      title: @section1.title,
      description: @section1.description,
      updated_at: I18n.l(@section1.updated_at, format: :long)
    }
    expect(@section1.as_full_json).to eq obj
  end

end
