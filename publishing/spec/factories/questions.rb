# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :question do
    text "Default question text"
    worked_out_answer "Default worked out answer"
  end
end
