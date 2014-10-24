# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :chapter_questions_set do
    title "Default question set title"
    chapter_quiz
  end
end
