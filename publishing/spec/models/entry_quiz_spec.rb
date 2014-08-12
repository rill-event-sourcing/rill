require 'rails_helper'

RSpec.describe EntryQuiz, :type => :model do
  it {is_expected.to validate_presence_of :course}
  it {is_expected.to have_many :questions}

  before do
    @eq = create(:entry_quiz)
  end

  it "should return an abbreviated uuid" do
    id = @eq.id.to_s
    expect(@eq.to_param).to eq id[0,8]
  end

  it "should be findable by an abbreviated uuid" do
    expect(EntryQuiz.find_by_uuid(@eq.to_param)).to eq @eq
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{EntryQuiz.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{EntryQuiz.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(EntryQuiz.find_by_uuid('1a31a31a', false)).to eq nil
  end

end
