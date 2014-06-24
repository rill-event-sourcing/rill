# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :multiple_choice_input do
    question
    sequence(:position) { |n| n }
  end
end
