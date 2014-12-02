# -*- coding: utf-8 -*-
module HtmlParseable
  require 'nokogiri'
  require 'nokogiri-styles'
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
      [/<math>(.*?)<\/math>/m, "<math></math>"]
    ]
  end

  def parse_page(attr)
    html = send(attr)
    fix_parsing_page.inject(html){|val, repl| val = val.to_s.gsub(repl.first, repl.last)}
  end

  def validation_hash
    transformer = lambda do |env|
      return unless env[:node_name] == 'img'
      node = env[:node]
      width = node.styles['width']
      return unless width
      node.unlink unless width =~ /^[0-9]+%$/
    end

    {
      :allow_doctype => true,

      :elements => %w[a b body br div h1 h2 h3 h4 h5 hr html i iframe img li math ol p span sub sup table td th tr u ul ],

      :attributes => {
        :all     => %w[class style],
        'a'      => %w[href],
        'iframe' => %w[allowfullscreen frameborder height src width],
        'img'    => %w[src]
      },

      :protocols => {
        'a'      => {'href' => ['https']},
        'iframe' => {'href' => ['https']},
        'img'    => {'src'  => ['https']}
      },

      :css => {
        :properties => %w[width margin-left]
      },

      :transformers => transformer
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
        if src =~ /^#{ asset_host }\//
          image = Image.find_by_url(src)
          if image
            errors << "`#{ src }` is not checked for dimensions." unless image.checked?
          else
            errors << "`#{ src }` is not found in our assets database."
          end
        else
          errors << "`#{ src }` is not a valid image source. It must be on #{ asset_host }"
        end
      else
        errors << "no 'src' value given for image"
      end
    end
    errors
  end

end
