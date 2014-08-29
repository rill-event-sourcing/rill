require 'rails_helper'

RSpec.describe Section, type: :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :chapter }
  it {is_expected.to have_many :subsections}
  it {is_expected.to have_many :questions}

  before do
    @section1 = create(:section, title: 'B', position: 2)
    @section2 = create(:section, title: 'C', position: 3)
    @section3 = create(:section, title: 'A', position: 1)
    @subsection1 = create(:subsection, title: "A", text: "A content", section: @section1)
    @subsection2 = create(:subsection, title: "B", text: "B content", section: @section1)
    @subsection3 = create(:subsection, title: "C", text: "C content", section: @section1)
  end

  it "should list sections in the right order" do
    expect(Section.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should provide an array with the meijerink criteria" do
    section = build(:section)
    section.meijerink_criteria = {"1F" => "0", "2F" => "0", "3F" => "0"}
    expect(section.selected_meijerink_criteria).to eq []
    section2 = build(:section)
    section2.meijerink_criteria = {"1F" => "0", "2F" => "1", "3F" => "0"}
    expect(section2.selected_meijerink_criteria).to eq ["2F"]
    section3 = build(:section)
    section3.meijerink_criteria = {"1F" => "1", "2F" => "0", "3F" => "1"}
    expect(section3.selected_meijerink_criteria).to eq ["1F", "3F"]
  end

  it "should not list trashed sections" do
    @section3.trash
    expect(Section.all.map(&:to_s)).to eq ['B', 'C']
    expect(Section.trashed.first).to eq @section3
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

  it "should return a full json object" do
    obj = {
      id: @section1.id,
      title: @section1.title,
      description: @section1.description,
      updated_at: I18n.l(@section1.updated_at, format: :long)
    }
    expect(@section1.as_full_json).to eq obj
  end

  it "should set correctly max position for the first created input" do
    expect(@section1.max_inputs).to eq nil
    @input = create(:line_input, inputable: @section1)
    expect(@section1.max_inputs).to eq 1
  end

  it "should increase max position when new inputs are generated" do
    @input = create(:line_input, inputable: @section1)
    max_inputs = @section1.max_inputs
    @input2 = create(:line_input, inputable: @section1)
    expect(@section1.max_inputs).to eq (max_inputs+1)
  end

  describe "enforcing constraints for publishing" do

    it "should make sure at least one meijerink criteria is selected" do
      section = build(:section, meijerink_criteria: {"1F" => "0", "2F" => "0", "3F" => "0"})
      section2 = build(:section, meijerink_criteria: {"1F" => "0", "2F" => "0", "3F" => "1"})

      expect(section.errors_when_publishing).to include "No Meijerink criteria selected in section '#{section.name}'"
      expect(section2.errors_when_publishing).not_to include "No Meijerink criteria selected in section '#{section2.name}'"
    end

    it "should make sure all inputs are referenced" do
      @input = create(:line_input, inputable: @section1)
      expect(@section1.errors_when_publishing).to include("Error in input referencing in section '#{@section1.name}', in '#{@section1.parent}'")
      @section1.subsections.first.text = "#{@input.name}"
      expect(@section1.errors_when_publishing).not_to include("Error in input referencing in section '#{@section1.name}', in '#{@section1.parent}'")
    end

    it "should make sure nonexisisting inputs are not referenced" do
      @input = create(:line_input, inputable: @section1)
      @subsection3 = create(:subsection, title: "A", text: "_INPUT_#{@input.position+1}_", section: @section1)

      expect(@section1.errors_when_publishing).to include("Nonexisting inputs referenced in section '#{@section1.name}', in '#{@section1.parent}'")

      @subsection3.destroy!
      @section1.reload

      @subsection3 = create(:subsection, title: "A", text: "_INPUT_#{@input.position}_", section: @section1)
      expect(@section1.errors_when_publishing).not_to include("Nonexisting inputs referenced in section '#{@section1.name}', in '#{@section1.parent}'")
    end

    it "should make sure there is at least one active question" do
      expect(@section1.errors_when_publishing).to include("No questions in section '#{ @section1.name }', in '#{ @section1.parent }'")

      @question1 = create(:question)
      @section1.questions << @question1
      expect(@section2.errors_when_publishing).not_to include("No questions in section '#{ @section1.name }', in '#{ @section1.parent }'")

      @question2 = create(:question, active: false)
      @section2.questions << @question2
      expect(@section2.errors_when_publishing).to include("No questions in section '#{ @section2.name }', in '#{ @section2.parent }'")
    end

    it "should make sure there is at least one subsection" do
      expect(@section1.errors_when_publishing).not_to include("No subsections in section '#{ @section1.name }', in '#{ @section1.parent }'")
      expect(@section2.errors_when_publishing).to include("No subsections in section '#{ @section2.name }', in '#{ @section2.parent }'")
    end
  end

end
