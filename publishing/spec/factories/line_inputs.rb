# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :line_input do
    question
    sequence(:position) { |n| n }
  end
end
