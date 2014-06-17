require 'rails_helper'

RSpec.describe Chapter, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :course }
  it {is_expected.to have_many :sections}

  before do
    @chapter1 = create(:chapter, title: 'B', position: 2)
    @chapter2 = create(:chapter, title: 'C', position: 3)
    @chapter3 = create(:chapter, title: 'A', position: 1)
  end

  it "should return title when asked for its string" do
    @chapter = build(:chapter)
    expect(@chapter.to_s).to eq @chapter.title
  end

  it "should list chapters in the right order" do
    expect(Chapter.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed chapters" do
    @chapter3.trash
    expect(Chapter.all.map(&:to_s)).to eq ['B', 'C']
    expect(Chapter.trashed.first).to eq @chapter3
  end

  it "should list recovered chapters" do
    @chapter3.trash
    expect(Chapter.all.map(&:to_s)).to eq ['B', 'C']
    @chapter3.recover
    expect(Chapter.all.map(&:to_s)).to eq ['A','B', 'C']
  end

  it "should list recovered chapters" do
    @chapter.trash
    expect(Chapter.all.map(&:to_s)).to eq ['B', 'C']
    @chapter.recover
    expect(Chapter.all.map(&:to_s)).to eq ['A','B', 'C']
  end

  it "should be activateable" do
    @chapter = build(:chapter, active: false)
    expect(@chapter.active).to eq false
    @chapter.activate
    expect(@chapter.active).to eq true
    @chapter.deactivate
    expect(@chapter.active).to eq false
  end

  it "should return an abbreviated uuid" do
    id = @chapter1.id.to_s
    expect(@chapter1.to_param).to eq id[0,8]
  end

  it "should return a json object" do
    obj = {id: @chapter3.id, title: @chapter3.title, sections: @chapter3.sections.map(&:as_json) }
    expect(@chapter3.as_json).to eq obj
  end

  it "should be findable by an abbreviated uuid" do
    expect(Chapter.find_by_uuid(@chapter3.to_param)).to eq @chapter3
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Chapter.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Chapter.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Chapter.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple chapters by an abbreviated uuid" do
    uuid = Chapter.first.id
    Chapter.all.each do |chapter|
      chapter.update_attribute :id, uuid[0,8] + chapter.id[8,28]
    end
    expect{Chapter.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

end
