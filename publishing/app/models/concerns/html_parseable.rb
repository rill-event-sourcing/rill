module HtmlParseable
  require 'nokogiri'

  extend ActiveSupport::Concern

  included do
  end

  module ClassMethods
  end

  def acceptable_errors
    ["Tag math invalid", "Tag minimap invalid", "EntityRef: expecting ';'"]
  end

  def html_value
    return "#{text}#{worked_out_answer}" if respond_to?(:text) && respond_to?(:worked_out_answer)
    return text  if respond_to?(:text)
    return value if respond_to?(:value)
  end

  def parse_page
    "<!DOCTYPE html>
<html>
<head>
</head>
<body>
#{html_value}
</body>
</html>"
  end

  def parsed_page
    @parsed_page ||= Nokogiri::HTML(parse_page)
  end

  def parse_errors
    parsed_page.errors.map(&:to_s) - acceptable_errors
  end

  def html_images
    parsed_page.css('img')
  end

  def image_errors
    errors = []
    html_images.each do |el|
      src = el["src"]
      unless src
        errors << "no 'src' given for image"
      end
    end
    errors
  end

end
