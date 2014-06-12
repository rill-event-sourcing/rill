require 'rails_helper'

RSpec.describe Section, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :chapter }
  it {is_expected.to have_many :subsections}

  before do
    @section1 = create(:section, title: 'B', position: 2)
    @section2 = create(:section, title: 'C', position: 3)
    @section3 = create(:section, title: 'A', position: 1)
  end

  it "should return title when asked for its string" do
    @section = build(:section)
    expect(@section.to_s).to eq @section.title
  end

  it "should list sections in the right order" do
    expect(Section.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed sections" do
    @section3.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    expect(Section.trashed.first).to eq @section3
  end

  it "should be activateable" do
    @section = build(:section)
    expect(@section.active).to eq false
    @section.activate
    expect(@section.active).to eq true
    @section.deactivate
    expect(@section.active).to eq false
  end

  it "should list recovered sections" do
    @section.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    @section.recover
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
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple Sections by an abbreviated uuid" do
    uuid = Section.first.id
    Section.all.each do |section|
      section.update_attribute :id, uuid[0,8] + section.id[8,28]
    end
    expect{Section.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return a json object" do
    obj = {id: @section1.id, title: @section1.title, subsections: @section1.subsections.map(&:as_json)}
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

  context "as a container for subsections" do

    before do
      @oneone = create(:subsection, title: "oneone", position: 1, stars: 1, section: @section1, description: "oneone desc" )
      @onetwo = create(:subsection, title: "onetwo", position: 2, stars: 1, section: @section1, description: "onetwo desc")
      @twoone = create(:subsection, title: "twoone", position: 1, stars: 2, section: @section1, description: "twoone desc")
      @twotwo = create(:subsection, title: "twotwo", position: 2, stars: 2, section: @section1, description: "twotwo desc")
      @threeone = create(:subsection, title: "threeone", position: 1, stars: 3, section: @section1, description: "threeone desc")
      @threetwo = create(:subsection, title: "threetwo", position: 2, stars: 3, section: @section1, description: "threetwo desc")
    end

    def update_first_subsection
      subsection = @section1.subsections.find_by_star(1).first.as_full_json
      subsection[:description] = "oneone modified desc"

      hashone = hashify [subsection.stringify,@onetwo.as_full_json.stringify]
      hashtwo = hashify [@twoone.as_full_json.stringify,@twotwo.as_full_json.stringify]
      hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]

      input = hashify([hashone, hashtwo, hashthree], true)

      @section1.subsections=input
    end

    it "should allow to update subsections" do
      update_first_subsection
      expect(@section1.subsections.find_by_star(1).first.as_full_json[:description]).to eq "oneone modified desc"
    end

    it "should correctly reflect the time of last update" do
      old_time = @section1.updated_at
      update_first_subsection
      expect(@section1.updated_at.to_f).to be > old_time.to_f
    end

    it "should respect the order of input subsections" do

      first_subsection = @section1.subsections.find_by_star(2).first
      last_subsection = @section1.subsections.find_by_star(2).last

      hashone = hashify [@oneone.as_full_json.stringify,@onetwo.as_full_json.stringify]
      hashtwo = hashify [last_subsection.as_full_json.stringify,first_subsection.as_full_json.stringify]
      hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]

      input = hashify([hashone, hashtwo, hashthree], true)

      @section1.subsections=input

      expect(@section1.subsections.find_by_star(2).first).to eq last_subsection
      expect(@section1.subsections.find_by_star(2).last).to eq first_subsection
    end


  end


end
