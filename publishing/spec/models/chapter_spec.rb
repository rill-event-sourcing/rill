require 'rails_helper'

RSpec.describe Chapter, :type => :model do
  it {is_expected.to validate_presence_of :title }
  it {is_expected.to validate_presence_of :course }
  # it {is_expected.to have_many :sections}

  before do
    create(:chapter, title: 'B', order: 2)
    create(:chapter, title: 'C', order: 3)
    @chapter = create(:chapter, title: 'A', order: 1)
  end

  it "should return title when asked for its string" do
    @chapter = build(:chapter)
    expect(@chapter.to_s).to eq @chapter.title
  end

  it "should list chapters in order of order" do
    expect(Chapter.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed chapters" do
    @chapter.trash
    expect(Chapter.all.map(&:to_s)).to eq ['B', 'C']
    expect(Chapter.trashed.first).to eq @chapter
  end

  it "should be activateable" do
    @chapter = build(:chapter)
    expect(@chapter.active).to eq false
    @chapter.activate
    expect(@chapter.active).to eq true
    @chapter.deactivate
    expect(@chapter.active).to eq false
  end

end
