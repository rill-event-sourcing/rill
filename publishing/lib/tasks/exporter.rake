namespace :exporter do
  desc "Export learning material to json"

  task :export => :environment  do
    require 'json'
    print JSON.pretty_generate(Course.first.to_publishing_format)
  end

  task :test_export => :environment do
    require 'json'
    published_format = JSON.parse(JSON.pretty_generate(Course.first.to_publishing_format))
    recursive_delete!(published_format,"id")
    doc_file = JSON.parse(File.read(Rails.root.join('..','resources','dev','material.json')))
    recursive_delete!(doc_file,"id")
    if published_format == doc_file
      p "Exporter: OK"
    else
      abort "The exported file and the versioned control documentation are different!"
    end
  end

  def recursive_delete!(node, key)
    if node.is_a?(Array) || node.is_a?(Hash)
      node.reject! { |e| e == key }
      node.each do |e|
        recursive_delete!(e, key)
      end
    end
  end
end
