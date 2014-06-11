require 'rails_helper'

RSpec.describe Subsection, :type => :model do
  it {is_expected.to validate_presence_of :section }
  it {is_expected.to validate_presence_of :stars }

  before do
    create(:subsection, title: "A", description: "A content", stars: 1)
    create(:subsection, title: "B", description: "B content", stars: 2)
    @subsection = create(:subsection, title: "C", description: "C content", stars: 3)
  end

  it "should return the title when asked for a string" do
    @subsection = build(:subsection)
    expect(@subsection.to_s).to eq @subsection.title
  end

  it "should return an abbreviated uuid" do
    @subsection = create(:subsection)
    id = @subsection.id.to_s
    expect(@subsection.to_param).to eq id[0..7]
  end

  it "should return a json object" do
    obj = {id: @subsection.id, title: @subsection.title}
    expect(@subsection.as_json).to eq obj
  end

  it "should return a full json object" do
    obj = {
      id: @subsection.id,
      position: @subsection.position,
      stars: @subsection.stars,
      title: @subsection.title,
      description: @subsection.description
      }
    expect(@subsection.as_full_json).to eq obj
  end

end
