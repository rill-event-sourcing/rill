# -*- coding: utf-8 -*-
module HtmlParseable
  require 'nokogiri'
  require 'sanitize'

  extend ActiveSupport::Concern

  included do
  end

  module ClassMethods
  end

  def fix_parsing_page
    [
     [/allowfullscreen/, "allowfullscreen=\"\""],
     ["\r", ""],
     [/<math>(.*?)<\/math>/m, "<math></math>"],
     [" < ", " &gt; "],
     [" > ", " &lt; "]
    ]
  end

  def parse_page(attr)
    html = send(attr)
    fix_parsing_page.inject(html){|val, repl| val = val.gsub(repl.first, repl.last) }
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

  def parse_errors(attr)
    page = parse_page(attr)
    parsed = Sanitize.fragment(page, validation_hash)
    lines1 = page.lines
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

  def html_images(attr)
    page = parse_page(attr)
    parsed_page = Nokogiri::HTML(page)
    parsed_page.css('img')
  end

  def image_errors(attr)
    asset_host = "https://assets.studyflow.nl"
    errors = []
    html_images(attr).each do |el|
      src = el["src"]
      if src
        errors << "`#{ src }` is not a valid image src. It must be on #{asset_host}/" unless src =~ /^#{ asset_host }\//
      else
        errors << "no 'src' given for image"
      end
    end
    errors
  end

end
