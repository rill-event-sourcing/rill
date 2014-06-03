require 'rails_helper'

RSpec.describe Course, :type => :model do

  it {is_expected.to validate_presence_of :name }
  it {is_expected.to validate_uniqueness_of :name }

  it "should return name when asked for its string" do
    @course = build(:course)
    expect(@course.to_s).to eq @course.name
  end

  it "should list courses in order of name" do
    create(:course, name: 'B')
    create(:course, name: 'C')
    create(:course, name: 'A')
    expect(Course.all.map(&:to_s)).to eq ['A', 'B', 'C']
  end

  it {is_expected.to have_many :chapters}

  it "should not list trashed courses" do
    create(:course, name: 'B')
    create(:course, name: 'C')
    @course = create(:course, name: 'A')
    @course.trash
    expect(Course.all.map(&:to_s)).to eq ['B', 'C']
    expect(Course.trashed.first).to eq @course
  end

  it "should be activateable" do
    @course = build(:course)
    expect(@course.active).to eq false
    @course.activate
    expect(@course.active).to eq true
    @course.deactivate
    expect(@course.active).to eq false
  end

end
