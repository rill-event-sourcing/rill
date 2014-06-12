require 'rails_helper'

RSpec.describe Section, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :chapter }
  it {is_expected.to have_many :subsections}

  before do
    create(:section, title: 'B', position: 2)
    create(:section, title: 'C', position: 3)
    @section = create(:section, title: 'A', position: 1)
  end


    it "should return title when asked for its string" do
      @section = build(:section)
      expect(@section.to_s).to eq @section.title
    end

    it "should list sections in the right order" do
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

    it "should return an abbreviated uuid" do
      id = @section.id.to_s
      expect(@section.to_param).to eq id[0..7]
    end

    it "should return a json object" do
      obj = {id: @section.id, title: @section.title, subsections: @section.subsections.map(&:as_json)}
      expect(@section.as_json).to eq obj
    end

    it "should return a full json object" do
      obj = {
        id: @section.id,
        title: @section.title,
        description: @section.description,
        updated_at: I18n.l(@section.updated_at, format: :long)
      }
      expect(@section.as_full_json).to eq obj
    end

  context "as a container for subsections" do

    before do
      @oneone = create(:subsection, title: "oneone", position: 1, stars: 1, section: @section, description: "oneone desc" )
      @onetwo = create(:subsection, title: "onetwo", position: 2, stars: 1, section: @section, description: "onetwo desc")
      @twoone = create(:subsection, title: "twoone", position: 1, stars: 2, section: @section, description: "twoone desc")
      @twotwo = create(:subsection, title: "twotwo", position: 2, stars: 2, section: @section, description: "twotwo desc")
      @threeone = create(:subsection, title: "threeone", position: 1, stars: 3, section: @section, description: "threeone desc")
      @threetwo = create(:subsection, title: "threetwo", position: 2, stars: 3, section: @section, description: "threetwo desc")
    end

    def update_first_subsection
      subsection = @section.subsections.find_by_star(1).first.as_full_json
      subsection[:description] = "oneone modified desc"

      hashone = hashify [subsection.stringify,@onetwo.as_full_json.stringify]
      hashtwo = hashify [@twoone.as_full_json.stringify,@twotwo.as_full_json.stringify]
      hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]

      input = hashify([hashone, hashtwo, hashthree], true)

      @section.subsections=input
    end

    it "should allow to update subsections" do
      update_first_subsection
      expect(@section.subsections.find_by_star(1).first.as_full_json[:description]).to eq "oneone modified desc"
    end

    it "should correctly reflect the time of last update" do
      old_time = @section.updated_at
      update_first_subsection
      expect(@section.updated_at.to_f).to be > old_time.to_f
    end

    it "should respect the order of input subsections" do

      first_subsection = @section.subsections.find_by_star(2).first
      last_subsection = @section.subsections.find_by_star(2).last

      hashone = hashify [@oneone.as_full_json.stringify,@onetwo.as_full_json.stringify]
      hashtwo = hashify [last_subsection.as_full_json.stringify,first_subsection.as_full_json.stringify]
      hashthree = hashify [@threeone.as_full_json.stringify,@threetwo.as_full_json.stringify]

      input = hashify([hashone, hashtwo, hashthree], true)

      @section.subsections=input

      expect(@section.subsections.find_by_star(2).first).to eq last_subsection
      expect(@section.subsections.find_by_star(2).last).to eq first_subsection
    end


  end


end
