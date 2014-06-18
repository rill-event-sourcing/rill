# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :multiple_choice_input do
    text "Default open question text"
    question
  end
end
