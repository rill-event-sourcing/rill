require 'rails_helper'

RSpec.describe Input, :type => :model do

  it {is_expected.to validate_presence_of :question }

end
