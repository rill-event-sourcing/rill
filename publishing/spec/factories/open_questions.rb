# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :open_question do
    text "Default open question text"
    section
  end
end
