# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :section do
    title "Default section title"
    description "Default section description"
    chapter
  end
end
