namespace :assets do

  desc "move images S3"
  task :s3 => :environment do
    p "before:"
    Rake::Task["assets:check"].execute
    Rake::Task["assets:move_s3"].execute
    p "after:"
    Rake::Task["assets:check"].execute
  end

  desc "copy images Gdoc"
  task :gdoc1 => :environment do
    Rake::Task["assets:find_gdocs"].execute
    Rake::Task["assets:move_gdocs"].execute
  end

  desc "update images Gdoc"
  task :gdoc2 => :environment do
    Rake::Task["assets:check"].execute
    Rake::Task["assets:find_gdocs"].execute
    Rake::Task["assets:update_gdocs"].execute
    Rake::Task["assets:check"].execute
  end

  desc "check images"
  task :check => :environment do
    require 'nokogiri'

    images = []
    [Question, Subsection, Choice].each do |html_type|
      html_type.find_in_batches do |items|
        items.each do |item|
          parsed_page = Nokogiri::HTML(parse_page(item))
          images += parsed_page.css('img').map{|el| el["src"]}
        end
      end
    end
    images.map{|img| img.split('/')[0,4].join('/')}.compact.uniq.sort.each do |img|
      p img
    end
  end

  ########################################################################################################################

  desc "move S3 to new S3 assets bucket"
  task :move_s3 => :environment  do
    connection = ActiveRecord::Base.connection
    connection.execute "UPDATE questions   SET text  = replace(text,  '#{old_asset_path1}', '#{new_asset_path1}')"
    connection.execute "UPDATE questions   SET text  = replace(text,  '#{old_asset_path2}', '#{new_asset_path2}')"

    connection.execute "UPDATE subsections SET text  = replace(text,  '#{old_asset_path1}', '#{new_asset_path1}')"
    connection.execute "UPDATE subsections SET text  = replace(text,  '#{old_asset_path2}', '#{new_asset_path2}')"

    connection.execute "UPDATE choices     SET value = replace(value, '#{old_asset_path1}', '#{new_asset_path1}')"
    connection.execute "UPDATE choices     SET value = replace(value, '#{old_asset_path2}', '#{new_asset_path2}')"

    connection.execute "UPDATE questions   SET worked_out_answer = replace(worked_out_answer, '#{old_asset_path1}', '#{new_asset_path1}')"
    connection.execute "UPDATE questions   SET worked_out_answer = replace(worked_out_answer, '#{old_asset_path2}', '#{new_asset_path2}')"
  end

  ########################################################################################################################

  desc "lookup image assets"
  task :lookup => :environment do
    [Question, Subsection, Choice].each do |html_type|
      html_type.find_in_batches do |items|
        items.each do |item|
          parsed_page = Nokogiri::HTML(parse_page(item))
          images = parsed_page.css('img').map{|el| el["src"]}
          images.each do |image|
            begin
              response = HTTParty.get(image, timeout: 1)
              if response.code == 200
                # p "."
              else
                p "image error in #{html_type} #{item.id} for '#{image}': #{response.code}"
              end
            rescue => ex
              p "image error in #{html_type} #{item.id} for '#{image}': #{ex}"
            end
          end
        end
      end
    end
  end


  desc "find Google Doc assets"
  task :find_gdocs => :environment do
    require 'nokogiri'
    found = 0
    @gdoc_hash = {}
    [Question, Subsection, Choice].each do |html_type|
      html_type.find_in_batches do |items|
        @gdoc_hash[html_type.to_s.downcase] ||= {}
        items.each do |item|
          parsed_page = Nokogiri::HTML(parse_page(item))
          images = parsed_page.css('img').map{|el| el["src"]}
          images.each_with_index do |image, index|
            next unless (image =~ /https:\/\/docs.google.com/)
            found += 1
            @gdoc_hash[html_type.to_s.downcase][item] ||= []
            gdoc_dir = "#{ item.id }"
            gdoc_file = "#{ index+1 }.png"
            gdoc = "#{ gdocs_path }#{ html_type.to_s.downcase }/#{ gdoc_dir }/#{ gdoc_file }"
            @gdoc_hash[html_type.to_s.downcase][item] << {old: image, gdoc: gdoc, tmp: gdoc_file}
          end
        end
      end
    end
    p "FOUND: #{ found } Gdoc images!"
  end

  desc "move Google Doc assets to S3 assets bucket"
  task :move_gdocs => :environment do
    @gdoc_hash.each do |html_type, items|
      items.each do |item, images|
        images.each do |image|
          temp = "/tmp/assets/#{ html_type }/#{ item.id }"
          cmd = "mkdir -p #{ temp }"
          `#{cmd}` # make temp dir
          cmd = "curl --progress-bar --output #{ temp }/#{ image[:tmp] } #{ image[:old] }"
          `#{cmd}` # get image from Google Docs
          cmd = "s3cmd put #{ temp }/#{ image[:tmp] } s3://#{ image[:gdoc] }"
          `#{cmd}` # upload to S3
        end
      end
    end
  end

  desc "update Google Doc assets to S3 assets in database"
  task :update_gdocs => :environment do
    @gdoc_hash.each do |html_type, items|
      items.each do |item, images|
        images.each do |image|
          old1 = image[:old]
          old2 = CGI.escapeHTML(old1)
          new = "https://#{ image[:gdoc] }"
          if html_type == 'question'
            p "question #{ item.id } #{ old1 } => #{ new }"
            item.update_attribute :text, item.text.gsub(old1, new)
            item.update_attribute :text, item.text.gsub(old2, new)
            item.update_attribute :worked_out_answer, item.worked_out_answer.gsub(old1, new)
            item.update_attribute :worked_out_answer, item.worked_out_answer.gsub(old2, new)
          elsif html_type == 'choice'
            p "choice #{ item.id } #{ old1 } => #{ new }"
            item.update_attribute :value, item.value.gsub(old1, new)
            item.update_attribute :value, item.value.gsub(old2, new)
          elsif html_type == 'subsection'
            p "subsection #{ item.id } #{ old1 } => #{ new }"
            item.update_attribute :text, item.text.gsub(old1, new)
            item.update_attribute :text, item.text.gsub(old2, new)
          else
            throw "wtf? unknown type: #{ html_type }"
          end
        end
      end
    end
  end

  ########################################################################################################################

  def old_asset_path1
    "https://s3.amazonaws.com/studyflow/rekenen/"
  end
  def new_asset_path1
    "https://assets.studyflow.nl/rekenen-old/"
  end

  def old_asset_path2
    "https://s3-eu-west-1.amazonaws.com/rekenen/"
  end
  def new_asset_path2
    "https://assets.studyflow.nl/rekenen/"
  end

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
