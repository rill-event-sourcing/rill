# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :entry_quiz do
    instructions "Default instructions for an entry quiz"
    feedback "Default feedback for an entry quiz"
    course
  end
end
