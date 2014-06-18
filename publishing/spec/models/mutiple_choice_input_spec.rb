require 'rails_helper'

RSpec.describe MultipleChoiceInput, type: :model do

  it {is_expected.to have_many :choices}

end
