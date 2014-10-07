# -*- coding: utf-8 -*-
namespace :calculator do

  desc "add calculator to selected sections"
  task :add => :environment do
    course = Course.first
    p "adding calculator for course: #{ course }"
    sections = []

    chapters = course.chapters.where(position: [7, 11, 14, 15, 16, 17, 18, 20, 21, 23, 24, 25, 26, 27, 28, 29])
    sections += chapters.map{|chapter| chapter.sections}.flatten

    chapter = course.chapters.where(position: 19).first
    sections += chapter.sections.where(position: [4, 5])

    sections.each do |section|
      p " - chapter #{ section.chapter.position }  - sectie #{ section.position }"
      section.questions.map{|q| q.update_attribute(:tools, {"pen_and_paper" => 1, "calculator" => 1}) }
    end
  end
end
