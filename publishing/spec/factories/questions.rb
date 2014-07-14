# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :question do
    text "Default question text"
    explanation "Default explanation"
    section
  end
end
