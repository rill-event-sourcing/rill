# -*- coding: utf-8 -*-
module HtmlParseable
  require 'nokogiri'
  require 'sanitize'

  extend ActiveSupport::Concern

  included do
  end

  module ClassMethods
  end

  def html_value
    return "#{text}#{worked_out_answer}" if respond_to?(:text) && respond_to?(:worked_out_answer)
    return text  if respond_to?(:text)
    return value if respond_to?(:value)
  end

  def fix_parsing_page
    [
     [/allowfullscreen/, "allowfullscreen=\"\""],
     ["\r", ""],
     [/<math\b[^>]*>(.*?)<\/math>/, "<math></math>"]
    ]
  end

  def parse_page
    fix_parsing_page.inject(html_value){|val, repl| val = val.gsub(repl.first, repl.last) }
  end

  def validation_hash
    {
      :allow_doctype => true,

      :elements => %w[html body a br b p span math h1 h2 h3 h4 h5 ul ol li u div img iframe i table tr th td sup sub],

      :attributes => {
        :all     => %w[class style],
        'a'      => %w[href],
        'iframe' => %w[src height width frameborder allowfullscreen],
        'img'    => %w[src]
      },

      :protocols => {
        'a'      => {'href' => ['https']},
        'img'    => {'src' => ['https']},
        'iframe' => {'href' => ['https']}
      },

      :css => {
        :properties => %w[width margin-left]
      }
    }
  end

  def parse_errors
    parsed = Sanitize.fragment(parse_page, validation_hash)
    lines1 = parse_page.lines
    lines2 = parsed.lines

    errors = []
    [lines1.length, lines2.length].max.times do |nr|
      line1 = lines1[nr].to_s.strip
      line2 = lines2[nr].to_s.strip
      errors << [line1, line2] unless line1 == line2
    end
    errors
  end

  #######################################################################################

  def html_images
    parsed_page = Nokogiri::HTML(parse_page)
    parsed_page.css('img')
  end

  def image_errors
    asset_host = "https://assets.studyflow.nl"
    errors = []
    html_images.each do |el|
      src = el["src"]
      if src
        errors << "`#{ src }` is not valid. src must be on #{asset_host}/" unless src =~ /^#{ asset_host }\//
      else
        errors << "no 'src' given for image"
      end
    end
    errors
  end

end
