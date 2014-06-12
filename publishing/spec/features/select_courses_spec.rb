require 'rails_helper'

feature "SelectCourses", type: :feature do
  before do
    create(:course, name: 'Math', active: true)
    create(:course, name: 'Engels', active: true)
  end

  scenario 'do Course selection', js: true do
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Engels', 'Math'])
    select('Math', from: 'course_id')
    visit root_path
    expect(page).to have_select('course_id', selected: 'Math')
    select('choose course', :from => 'course_id')
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Engels', 'Math'])
    expect(Course.current).to eq nil
  end

end
