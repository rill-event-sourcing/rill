require 'rails_helper'

feature "SelectCourses", :type => :feature do
  scenario 'Visit home page' do
    visit root_path
    page.should have_content('Home')
  end

  scenario 'Course selection' do
    visit root_path
    page.should have_content('Home')
  end

end
