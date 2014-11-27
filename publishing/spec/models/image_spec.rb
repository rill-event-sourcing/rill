require 'rails_helper'

RSpec.describe Image, :type => :model do
  it { should validate_presence_of(:url) }
  it { should validate_uniqueness_of(:url) }

  before do
  end

  it "should scope to checked images" do
    image1 = Image.create(url: 1, status: "checked")
    image2 = Image.create(url: 2)
    image3 = Image.create(url: 3, status: "checked")
    expect(Image.checked).to eq [image1, image3]
  end

  it "should scope to outdated images" do
    now = DateTime.now
    image1 = Image.create(url: 1, checked_at: now - 20.days)
    image2 = Image.create(url: 2)
    image3 = Image.create(url: 3, checked_at: now)
    expect(Image.outdated(now - 1.days)).to eq [image1, image2]
  end

  it "should return the url as name" do
    image = Image.new(url: "htps://test.nl/blabla/test.jpg")
    expect(image.to_s).to eq "htps://test.nl/blabla/test.jpg"
  end

  it "should return the bucket folder" do
    image = Image.new(url: 4)
    expect(image.bucket).to eq "#{ Rails.root }/bucket"
  end

  it "should return the path of it's url as path" do
    image = Image.new(url: "htps://test.nl/blabla/test.jpg")
    expect(image.path).to eq "/blabla/test.jpg"
  end

  it "should return the path to the asset file as filename" do
    image = Image.new(url: "htps://test.nl/blabla/test.jpg")
    expect(image.filename).to eq "#{ Rails.root }/bucket/blabla/test.jpg"
  end

end
