class CheckingController < ApplicationController

  before_action :set_breadcrumb

  def index
    @parse_errors = {}
    @image_errors = {}

    Subsection.find_in_batches.each do |items|
      items.each do |item|
        perr = item.parse_errors(:text)
        ierr = item.image_errors(:text)
        ref = %(<a href="/chapters/#{item.section.chapter.to_param}/sections/#{item.section.to_param}/subsections">#{item.reference}</a>).html_safe
        @parse_errors[ref] = perr if ref && perr.any?
        @image_errors[ref] = ierr if ref && ierr.any?
      end
    end
    Question.find_in_batches.each do |items|
      items.each do |item|
        perr = item.parse_errors(:text)
        ierr = item.image_errors(:text)
        if item.quizzable.is_a?(EntryQuiz)
          ref = %(<a href="/entry_quiz/entry_quiz_questions/#{item.to_param}/edit">#{item.reference}</a>).html_safe
        elsif item.quizzable.is_a?(Section)
          ref = %(<a href="/chapters/#{item.quizzable.chapter.to_param}/sections/#{item.quizzable.to_param}/questions/#{item.to_param}/edit">#{item.reference}</a>).html_safe
        elsif item.quizzable.is_a?(ChapterQuestionsSet)
          ref = %(<a href="/chapters/#{item.quizzable.chapter_quiz.chapter.to_param}/chapter_quiz/chapter_questions_sets/#{item.quizzable.to_param}/chapter_quiz_questions/#{item.to_param}/edit">#{item.reference}</a>).html_safe
        end
        @parse_errors[ref] = perr if ref && perr.any?
        @image_errors[ref] = ierr if ref && ierr.any?
      end
    end
  end

  def set_breadcrumb
    @crumbs = [{name: Course.current.name, url: root_path}] if Course.current
  end
end
