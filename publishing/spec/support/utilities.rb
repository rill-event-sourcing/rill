class Hash
  def stringify
    inject({}) do |options, (key, value)|
      options[key.to_s] = value.to_s
      options
    end
  end
end



def hashify(array,start_from_one=false)
  if start_from_one
    indices = 1...array.size+1
  else
    indices = 0...array.size
  end
  Hash[indices.map(&:to_s).zip array]
end
