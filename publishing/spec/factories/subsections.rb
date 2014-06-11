# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :subsection do
      title "Default subsection title"
      description "Default subsection description"
      stars 2
      sequence(:position)
      section
  end
end
