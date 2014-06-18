require 'rails_helper'

RSpec.describe Answer, type: :model do

  it {is_expected.to validate_presence_of :value }
  it {is_expected.to belong_to :line_input }

end
