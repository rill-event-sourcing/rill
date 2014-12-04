require 'rails_helper'

RSpec.describe Subsection, type: :model do
  it {is_expected.to validate_presence_of :section }

  before do
    @subsection1 = create(:subsection, title: "A", text: "A content")
    @subsection2 = create(:subsection, title: "B", text: "B content")
    @subsection3 = create(:subsection, title: "C", text: "C content")
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

  it "should return a full json object" do
    obj = {
      id: @subsection1.id,
      position: @subsection1.position,
      title: @subsection1.title,
      text: @subsection1.text
    }
    expect(@subsection1.as_full_json).to eq obj
  end

  describe "enforcing constraints for publishing" do

    it "should make sure text is nonempty" do
      @section1 = create(:section)
      @subsection1.section = @section1
      expect(@subsection1.errors_when_publishing).not_to include("No content in subsection of section '#{ @section1.name }', in '#{ @section1.parent }'")
      @subsection1.text = nil
      expect(@subsection1.errors_when_publishing).to include("No content in subsection of section '#{ @section1.name }', in '#{ @section1.parent }'")
    end

    it "should accept valid width css property of image" do
      @section1 = create(:section)
      @subsection1.section = @section1
      @subsection1.text = %(<img src="https://assets.studyflow.nl/bg.jpg" style="width: 80%">)
      errors = @subsection1.parse_errors(:text)
      expect(errors).to eq []
    end

    it "should reject invalid width css property of image" do
      @section1 = create(:section)
      @subsection1.text = %(<img src="https://assets.studyflow.nl/bg.jpg" style="width: 200px">)
      @subsection1.section = @section1
      errors = @subsection1.parse_errors(:text)
      expect(errors).to eq [[%(<img src="https://assets.studyflow.nl/bg.jpg" style="width: 200px">), ""]]
    end

  end

  describe "stripping tabs and newlines from titles" do
    it "should not include newlines or tabs in the title" do
      @subsection = build(:subsection, title: "\t\n\ttest\n\t\n")
      expect(@subsection.to_publishing_format[:title]).to eq "test"
    end
  end

end
