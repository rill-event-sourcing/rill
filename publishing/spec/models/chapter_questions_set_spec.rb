require 'rails_helper'

RSpec.describe ChapterQuestionsSet, :type => :model do

  before do
    @ch1 = create(:chapter)
    @q1 = create(:chapter_quiz, chapter: @ch1)
    @q1s1 = create(:chapter_questions_set, chapter_quiz: @q1, title: "Q1S1")
    @q1s2 = create(:chapter_questions_set, chapter_quiz: @q1, title: "Q1S2")
  end

  it {is_expected.to validate_presence_of :chapter_quiz }
  it {is_expected.to have_many :questions}

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{ChapterQuestionsSet.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{ChapterQuestionsSet.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(ChapterQuestionsSet.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple question sets by an abbreviated uuid" do
    uuid = ChapterQuestionsSet.first.id
    ChapterQuestionsSet.all.each do |qs|
      qs.update_attribute :id, uuid[0,8] + qs.id[8,28]
    end
    expect{ChapterQuestionsSet.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should return its name" do
    expect(@q1s1.to_s).to eq @q1s1.title
  end


  it "should return an abbreviated uuid" do
    id = @q1s1.id.to_s
    expect(@q1s1.to_param).to eq id[0,8]
  end

  it "should publish its title" do
    expect(@q1s1.to_publishing_format.has_key?(:title)).to eq true
    expect(@q1s1.to_publishing_format[:title]).to eq @q1s1.title
  end

  describe "enforcing constraints for publishing" do

    it "should make sure there are questions" do
      @q1 = create(:question, quizzable: @q1s1)
      @q1s1.reload
      expect(@q1s1.errors_when_publishing).not_to include "No questions in the chapter quiz for chapter '#{@ch1.title}'"
      expect(@q1s2.errors_when_publishing).to include "No questions in the chapter quiz for chapter '#{@ch1.title}'"
    end

  end

  describe "stripping tabs and newlines from titles" do
    it "should not include newlines or tabs in the title" do
      @chapter_questions_set = build(:chapter_questions_set, title: "\t\n\ttest\n\t\n")
      expect(@chapter_questions_set.to_publishing_format[:title]).to eq "test"
    end
  end

end
