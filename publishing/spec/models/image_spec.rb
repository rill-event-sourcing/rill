require 'rails_helper'

RSpec.describe Image, :type => :model do
  it { should validate_presence_of(:path) }
  it { should validate_uniqueness_of(:path) }

  before do
  end

  it "should scope to checked images" do
    image1 = Image.create(path: "1.jpg", status: "checked")
    image2 = Image.create(path: "2.jpg")
    image3 = Image.create(path: "3.jpg", status: "checked")
    expect(Image.checked).to eq [image1, image3]
  end

  it "should find the immage by url" do
    image1 = Image.create(path: "/1.jpg")
    expect(Image.find_by_url("https://assets.studyflow.nl/1.jpg")).to eq image1
  end

  it "should scope to outdated images" do
    now = DateTime.now
    image1 = Image.create(path: "1.jpg", checked_at: now - 20.days)
    image2 = Image.create(path: "2.jpg")
    image3 = Image.create(path: "3.jpg", checked_at: now)
    expect(Image.outdated(now - 1.days)).to eq [image1, image2]
  end

  it "should return the path as name" do
    image = Image.new(path: "/blabla/test.jpg")
    expect(image.to_s).to eq "/blabla/test.jpg"
  end

  it "should return the bucket folder" do
    expect(Image.bucket_dir).to eq "#{ Rails.root }/bucket"
  end

  it "should return the path to the asset file as filename" do
    image = Image.new(path: "/blabla/test.jpg")
    expect(image.filename).to eq "#{ Rails.root }/bucket/blabla/test.jpg"
  end

  it "should return true when staus == checked" do
    image = Image.new(path: "4.jpg")
    expect(image.checked?).to eq false
    image = Image.new(path: "5.jpg", status: "NOTchecked")
    expect(image.checked?).to eq false
    image = Image.new(path: "6.jpg", status: "checked")
    expect(image.checked?).to eq true
  end

end
