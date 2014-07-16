# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :subsection do
      title "Default subsection title"
      text "Default subsection text"
      sequence(:position)
      section
  end
end
