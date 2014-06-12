require 'rails_helper'

RSpec.describe Chapter, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :course }
  it {is_expected.to have_many :sections}

  before do
    create(:chapter, title: 'B', position: 2)
    create(:chapter, title: 'C', position: 3)
    @chapter = create(:chapter, title: 'A', position: 1)
  end

  it "should return title when asked for its string" do
    @chapter = build(:chapter)
    expect(@chapter.to_s).to eq @chapter.title
  end

  it "should list chapters in the right order" do
    expect(Chapter.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed chapters" do
    @chapter.trash
    expect(Chapter.all.map(&:to_s)).to eq ['B', 'C']
    expect(Chapter.trashed.first).to eq @chapter
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
    id = @chapter.id.to_s
    expect(@chapter.to_param).to eq id[0..7]
  end

  it "should return a json object" do
    obj = {id: @chapter.id, title: @chapter.title, sections: @chapter.sections.map(&:as_json) }
    expect(@chapter.as_json).to eq obj
  end

end
