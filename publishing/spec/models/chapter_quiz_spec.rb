require 'rails_helper'

RSpec.describe ChapterQuiz, :type => :model do

  it {is_expected.to validate_presence_of :chapter }
  it {is_expected.to have_many :questions}
  it {is_expected.to have_many :chapter_questions_sets}

  before do
    @ch1 = create(:chapter)
    @ch2 = create(:chapter)
    @q1 = create(:chapter_quiz, chapter: @ch1)
    @q2 = create(:chapter_quiz, chapter: @ch2)

    @q1s1 = create(:chapter_questions_set, chapter_quiz: @q1, title: "Q1S1")
    @q1s2 = create(:chapter_questions_set, chapter_quiz: @q1, title: "Q1S2")
    @q2s1 = create(:chapter_questions_set, chapter_quiz: @q2, title: "Q2S1")
    @q2s2 = create(:chapter_questions_set, chapter_quiz: @q2, title: "Q2S2")
  end

  it "should list questions set in the right order" do
    expect(@q1.chapter_questions_sets).to eq [@q1s1,@q1s2]
    @q1s1.move_lower
    @q1.reload
    expect(@q1.chapter_questions_sets).to eq [@q1s2,@q1s1]
  end

  it "should return an abbreviated uuid" do
    id = @q1.id.to_s
    expect(@q1.to_param).to eq id[0,8]
  end

  it "should return its name" do
    expect(@q1.to_s).to eq  "#{@ch1} - Chapter Quiz"
  end


  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{ChapterQuiz.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{ChapterQuiz.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(ChapterQuiz.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple Sections by an abbreviated uuid" do
    uuid = ChapterQuiz.first.id
    ChapterQuiz.all.each do |chapter_quiz|
      chapter_quiz.update_attribute :id, uuid[0,8] + chapter_quiz.id[8,28]
    end
    expect{ChapterQuiz.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

  it "should publish its questions sets" do
    expect(@q1.to_publishing_format).to eq @q1.chapter_questions_sets.map(&:to_publishing_format)
  end

end
