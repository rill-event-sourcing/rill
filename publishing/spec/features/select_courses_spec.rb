require 'rails_helper'

feature "SelectCourses", type: :feature do
  before do
    create(:course, name: 'Math')
    create(:course, name: 'Engels')
  end

  scenario 'do Course selection', js: true do
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Engels', 'Math'])

    select('Math', from: 'course_id')
    expect(page).to have_select('course_id', selected: 'Math')
    visit root_path
    expect(page).to have_select('course_id', selected: 'Math')

    select('Engels', from: 'course_id')
    expect(page).to have_select('course_id', selected: 'Engels')
    visit root_path
    expect(page).to have_select('course_id', selected: 'Engels')

    select('choose course', :from => 'course_id')
    expect(page).to have_select('course_id', options: ['choose course', 'Engels', 'Math'])
    visit root_path
    expect(page).to have_select('course_id', options: ['choose course', 'Engels', 'Math'])
  end

end
