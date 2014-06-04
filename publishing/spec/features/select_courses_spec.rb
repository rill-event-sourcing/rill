require 'rails_helper'

feature "SelectCourses", :type => :feature do
  before do
    create(:course, name: 'Math')
    create(:course, name: 'Engels')
  end

  scenario 'Visit home page' do
    visit root_path
    expect(page).to have_content('Home')
  end

  scenario 'Course selection', js: true do
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Math', 'Engels'])
    select('Math', :from => 'course_id')
    visit root_path
    expect(page).to have_select('course_id', :selected => 'Math')
  end

  # scenario 'Course list' do
  #   visit courses_path
  #   expect(page).to have_content('Courses')
  #   expect(page).to have_content('New Course')
  # end

end
