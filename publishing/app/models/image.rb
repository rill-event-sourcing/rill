class Image < ActiveRecord::Base
  require "digest"

  validates :url, presence: true, uniqueness: true

  scope :checked,  -> { where(status: "checked") }
  scope :outdated, -> (time) { where(["checked_at IS NULL OR checked_at < ?", time]) }

  attr_accessor :file_sha

  def to_s
    "#{ url }"
  end

  def bucket
    StudyflowPublishing::Application.config.bucket_dir
  end

  def path
    URI.parse(url).path
  end

  def filename
    "#{ bucket }#{ path }"
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
        p "checking dimensions of: #{ path }"
        image = MiniMagick::Image.open(filename)
        dimension = image.dimensions
        update_attributes sha: file_sha,
                          width: dimension.first,
                          height: dimension.last
      else
        # p "no difference of file: #{ path }"
      end
      update_attributes status: :checked,
                        checked_at: DateTime.now
    rescue Exception => ex
      p "ERROR checking image: #{ path } => #{ex}"
      update_attributes status: :not_checked,
                        checked_at: nil
    end
  end

end
