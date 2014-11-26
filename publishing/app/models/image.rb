class Image < ActiveRecord::Base
  require "digest"

  validate :url, presence: true, unique: true

  scope :outdated, -> (time) { where(["checked_at IS NULL OR checked_at > ?", time]) }

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

  def not_checked?
    status != "checked"
  end

  def check
    begin
      file = File.read(filename)
      file_sha = Digest::MD5.hexdigest(file)
      if not_checked? || file_sha != sha
        p "checking: #{ path }"
        image = MiniMagick::Image.open(filename)
        dimension = image.dimensions
        update_attributes sha: file_sha,
                          width: dimension.first,
                          height: dimension.last,
                          status: :checked,
                          checked_at: DateTime.now
      end
    rescue Exception => ex
      p ex
      update_attributes status: :not_checked,
                        checked_at: DateTime.now
    end
  end

end
