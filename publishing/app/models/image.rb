class Image < ActiveRecord::Base
  require "digest"
 
  validates :path, presence: true, uniqueness: true
  validate :validate_extension, on: :create

  scope :checked,  -> { where(status: "checked") }
  scope :outdated, -> (time) { where(["checked_at IS NULL OR checked_at < ?", time]) }

  attr_accessor :file_sha

  def self.find_by_url(url)
    images = Image.where(path: path_of_url(url))
    images.first
  end

  def validate_extension
    errors.add(:path, "is not an image") unless ['png', 'jpg', 'gif'].include?(extension)
  end

  def to_s
    "#{ path }"
  end

  def extension
    return unless path
    path.to_s.split('.').last
  end

  def self.bucket_dir
    StudyflowPublishing::Application.config.bucket_dir
  end

  def self.path_of_url(url)
    begin
      URI.unescape(URI.parse(url).path).gsub("\+", " ")
    rescue URI::InvalidURIError
      p "invalid URI: #{ url }"
    end
  end

  def filename
    URI.unescape("#{ Image.bucket_dir }#{ path }").gsub("\+", " ")
  end

  def checked?
    status == "checked"
  end

  def needs_update?
    # new image or no dimensions or changed file
    !sha || !width || !height || sha != file_sha
  end

  def file_sha
    @file_sha ||= Digest::MD5.hexdigest(File.read(filename))
  end

  def check
    begin
      if needs_update?
        image = MiniMagick::Image.open(filename)
        dimension = image.dimensions
        update_attributes sha: file_sha,
                          width: dimension.first,
                          height: dimension.last
      end
      update_attributes status: :checked,
                        checked_at: DateTime.now
    rescue Exception => ex
      p "ERROR checking image: #{ path }, file: #{ path }"
      update_attributes status: :not_checked,
                        checked_at: nil
    end
  end

end
