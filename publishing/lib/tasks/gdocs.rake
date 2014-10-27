# -*- coding: utf-8 -*-
namespace :gdocs do

  desc "cleanup images Gdoc"
  task :gdoc1 => :environment do
    Rake::Task["gdocs:find"].execute
    Rake::Task["gdocs:copy"].execute
  end

  desc "cleanup images Gdoc"
  task :gdoc2 => :environment do
    Rake::Task["gdocs:check"].execute
    Rake::Task["gdocs:find"].execute
    Rake::Task["gdocs:move"].execute
    Rake::Task["gdocs:check"].execute
  end

  desc "check images"
  task :check => :environment do
    require 'nokogiri'

    images = []
    [Question, Subsection, Choice].each do |html_type|
      html_type.find_in_batches do |items|
        items.each do |item|
          parsed_page = Nokogiri::HTML(parse_page(item))
          parsed_page.css('img').map{|el| el["src"]}.each do |image|
            next unless (image =~ /https:\/\/assets.studyflow.nl\/gdocs\//)
            images << image
          end
        end
      end
    end
    images.map{|img| img.split('/')[0,5].join('/')}.compact.uniq.sort.each do |img|
      p img
    end
  end

  ########################################################################################################################

  def clean_filename(item)
    name = "#{item.name}" if item.is_a? Question
    name ||= item.to_s.downcase.squish
    name = name.gsub(".", "")
    name = name.gsub("&", "en")
    name = name.gsub("=", "is")
    name = name.gsub("+", "plus")
    name = name.gsub("-", "min")
    name = name.gsub("÷", "delen")
    name = name.gsub("×", "keer")
    name = name.gsub(" ", "-")
    name = name.gsub(/[àáâãäå]/,'a')
    name = name.gsub(/[èéêë]/,'e')
    name = name.gsub(/[^-0-9a-z]/i, '')
    return "#{ item.position.to_i }-#{ name }" if item.is_a?(Section)
    return "#{ item.position.to_i + 1}-#{ name }" if item.is_a?(Subsection)
    name
  end

  desc "find Google Doc assets"
  task :find => :environment do
    require 'nokogiri'
    found = 0
    @gdoc_hash = {}
    Course.first.chapters.each do |chapter|
      # p "################## #{clean_filename chapter} ##########################"
      chapter.sections.each do |section|
        dir = "#{clean_filename chapter}/#{clean_filename section}"
        section.subsections.each do |subsection|
          @gdoc_hash[:subsections] ||= {}
          dir2 = "#{dir}/uitleg/#{clean_filename subsection}/"
          parsed_page = Nokogiri::HTML(parse_page(subsection))
          images = parsed_page.css('img').map{|el| el["src"]}
          images.each_with_index do |image, index|
            next unless (image =~ /https:\/\/assets.studyflow.nl\/gdocs\//)
            found += 1
            gdoc = "https://#{gdocs_path}#{dir2}#{index+1}.png"
            @gdoc_hash[:subsections][subsection] ||= []
            @gdoc_hash[:subsections][subsection] << {old: image, gdoc: gdoc}
          end
        end
        section.questions.each do |question|
          @gdoc_hash[:questions] ||= {}
          dir2 = "#{dir}/vragen/#{clean_filename question}/"
          parsed_page = Nokogiri::HTML(parse_page(question))
          images = parsed_page.css('img').map{|el| el["src"]}
          images.each_with_index do |image, index|
            next unless (image =~ /https:\/\/assets.studyflow.nl\/gdocs\//)
            found += 1
            gdoc = "https://#{gdocs_path}#{dir2}#{index+1}.png"
            @gdoc_hash[:questions][question] ||= []
            @gdoc_hash[:questions][question] << {old: image, gdoc: gdoc}
          end

          choices = question.multiple_choice_inputs.map(&:choices).flatten
          choices.each do |choice|
            @gdoc_hash[:choices] ||= {}
            dir2 = "#{dir}/vragen/#{clean_filename question}/"
            parsed_page = Nokogiri::HTML(parse_page(choice))
            images = parsed_page.css('img').map{|el| el["src"]}
            images.each_with_index do |image, index|
              next unless (image =~ /https:\/\/assets.studyflow.nl\/gdocs\//)
              found += 1
              gdoc = "https://#{gdocs_path}#{dir2}input-#{choice.multiple_choice_input.position}-#{index+1}.png"
              @gdoc_hash[:choices][choice] ||= []
              @gdoc_hash[:choices][choice] << {old: image, gdoc: gdoc}
            end
          end
        end
      end
    end
    p "FOUND: #{ found } Gdoc images!"
  end

  desc "copy S3 Google Doc assets to new S3 assets location"
  task :copy => :environment do
    @gdoc_hash.each do |html_type, items|
      items.each do |item, images|
        images.each do |image|
          cmd = "s3cmd get #{ image[:old].gsub("https://", "s3://") } temp.png"
          p cmd
          `#{cmd}`
          cmd = "s3cmd put temp.png #{ image[:gdoc].gsub("https://", "s3://") }"
          p cmd
          `#{cmd}`
          cmd = "rm temp.png"
          p cmd
          `#{cmd}`
          # throw 'ok'
        end
      end
    end
  end

  desc "update Google Doc assets in database"
  task :move => :environment do
    @gdoc_hash.each do |html_type, items|
      items.each do |item, images|
        images.each do |image|
          if html_type == :questions
            p "question #{ item.id } #{ image[:old] } => #{ image[:gdoc] }"
            item.update_attribute :text, item.text.gsub(image[:old], image[:gdoc])
            item.update_attribute :worked_out_answer, item.worked_out_answer.gsub(image[:old], image[:gdoc])
          elsif html_type == :choices
            p "choice #{ item.id } #{ image[:old] } => #{ image[:gdoc] }"
            item.update_attribute :value, item.value.gsub(image[:old], image[:gdoc])
          elsif html_type == :subsections
            p "subsection #{ item.id } #{ image[:old] } => #{ image[:gdoc] }"
            item.update_attribute :text, item.text.gsub(image[:old], image[:gdoc])
          else
            throw "wtf? unknown type: #{ html_type }"
          end
        end
      end
    end
  end

  ########################################################################################################################

  def gdocs_path
    "assets.studyflow.nl/gdocs/"
  end

  def html_value(item)
    return "#{item.text}#{item.worked_out_answer}" if item.respond_to?(:text) && item.respond_to?(:worked_out_answer)
    return item.text  if item.respond_to?(:text)
    return item.value if item.respond_to?(:value)
  end

  def parse_page(item)
    "<html>
<head>
</head>
<body>
#{ html_value(item) }
</body>
</html>"
  end

end
