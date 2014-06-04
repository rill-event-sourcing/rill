# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :course do
    sequence(:name) { |n| "Default course name#{n}" }
  end
end
