require 'rails_helper'

RSpec.describe Course, type: :model do

  it {is_expected.to validate_presence_of :name }
  it {is_expected.to validate_uniqueness_of :name }
  it {is_expected.to have_many :chapters}

  before do
    create(:course, name: 'B')
    create(:course, name: 'C')
    @course = create(:course, name: 'A')
    @chapter = create(:chapter, course: @course, active: true)
  end

  it "should return name when asked for its string" do
    @course = build(:course)
    expect(@course.to_s).to eq @course.name
  end

  it "should list courses in order of name" do
    expect(Course.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it "should not list trashed courses" do
    @course.trash
    expect(Course.all.map(&:to_s)).to eq ['B', 'C']
    expect(Course.trashed.first).to eq @course
  end

  it "should list recovered courses" do
    @course.trash
    expect(Course.all.map(&:to_s)).to eq ['B', 'C']
    @course.recover
    expect(Course.all.map(&:to_s)).to eq ['A','B', 'C']
  end

  it "should be activateable" do
    @course = build(:course, active: false)
    expect(@course.active).to eq false
    @course.activate
    expect(@course.active).to eq true
    @course.deactivate
    expect(@course.active).to eq false
  end

  it "should make sure that an entry quiz is present" do
    @course = build(:course)
    expect(@course.errors_when_publishing).to include "No entry quiz for the course"
    @eq = build(:entry_quiz, course: @course)
    expect(@course.errors_when_publishing).not_to include "No entry quiz for the course"
  end

end
