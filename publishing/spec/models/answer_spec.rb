require 'rails_helper'

RSpec.describe Answer, type: :model do

  it {is_expected.to belong_to :line_input }


  before do
    @answer = create(:answer)
    @answer2 = create(:answer)
  end

  it "should return an abbreviated uuid" do
    id = @answer.id.to_s
    expect(@answer.to_param).to eq id[0,8]
  end

  it "should throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid" do
    expect{Answer.find_by_uuid('1a31a31a')}.to raise_error(ActiveRecord::RecordNotFound)
  end

  it "should not throw an ActiveRecord::RecordNotFound when not found by an abbreviated uuid with 'with_404' = false" do
    expect{Answer.find_by_uuid('1a31a31a', false)}.not_to raise_error
    expect(Answer.find_by_uuid('1a31a31a', false)).to eq nil
  end

  it "should throw an StudyflowPublishing::ShortUuidDoubleError when found multiple answers by an abbreviated uuid" do
    uuid = Answer.first.id
    Answer.all.each do |answer|
      answer.update_attribute :id, uuid[0,8] + answer.id[8,28]
    end
    expect{Answer.find_by_uuid(uuid[0,8])}.to raise_error(StudyflowPublishing::ShortUuidDoubleError)
  end

end
