# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :chapter do
    title "Default chapter name"
    description "Default chapter description"
    course
  end
end
