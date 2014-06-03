require 'rails_helper'

feature "SelectCourses", :type => :feature do
  before do

  end

  scenario 'Visit home page' do
    visit root_path
    expect(page).to have_content('Home')
  end

  scenario 'Course selection' do
    visit root_path
    expect(page).to have_content('choose course')
  end

  scenario 'Course list' do
    visit courses_path
    expect(page).to have_content('Listing courses')
    expect(page).to have_content('New Course')
    select_by_value 'course_id', 'Math'
  end

end
