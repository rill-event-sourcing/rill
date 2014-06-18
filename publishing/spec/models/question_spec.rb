require 'rails_helper'

RSpec.describe Question, :type => :model do

  it {is_expected.to validate_presence_of :text }
  it {is_expected.to validate_presence_of :section }

end
