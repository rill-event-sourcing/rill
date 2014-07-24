class Hash
  def stringify
    inject({}) do |options, (key, value)|
      options[key.to_s] = value.to_s
      options
    end
  end
end

def hashify(array_of_subsections)
  output_hash = {}
  array_of_subsections.each do |subsection|
    output_hash["#{subsection.id}"] = {id: subsection.id, position: subsection.position, title: subsection.title, text: subsection.text}
  end
  output_hash
end
