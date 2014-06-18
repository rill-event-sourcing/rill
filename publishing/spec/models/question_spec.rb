require 'rails_helper'

RSpec.describe OpenQuestion, :type => :model do

  it {is_expected.to validate_presence_of :text }
  it {is_expected.to validate_presence_of :section }

end
