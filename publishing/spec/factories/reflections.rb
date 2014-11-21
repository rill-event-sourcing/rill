# Read about factories at https://github.com/thoughtbot/factory_girl

FactoryGirl.define do
  factory :reflection do
    section { |a| a.association(:section) }
  end
end
