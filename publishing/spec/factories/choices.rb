# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :choice do
    value "Default multiple choice value"
    multiple_choice_input
  end
end
