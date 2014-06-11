require 'rails_helper'

feature "SelectCourses", :type => :feature do
  before do
    create(:course, name: 'Math', active: true)
    create(:course, name: 'Engels', active: true)
  end

  scenario 'Course selection', js: true do
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Math', 'Engels'])
    select('Math', :from => 'course_id')
    visit root_path
    expect(page).to have_select('course_id', :selected => 'Math')
  end

end
