require 'rails_helper'

RSpec.describe Choice, :type => :model do

  it {is_expected.to validate_presence_of :value }
  it {is_expected.to belong_to :multiple_choice_question}

end