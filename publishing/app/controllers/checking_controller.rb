class CheckingController < ApplicationController

  before_action :set_breadcrumb

  def index
    unless Course.current
      redirect_to root_path
      return
    end
    course = Course.current
    @errors = {}
    course.chapters.each do |chapter|
      @errors[chapter.title] ||= {}
      @errors[chapter.title][:html] ||= {}
      @errors[chapter.title][:images] ||= {}

      quiz = chapter.chapter_quiz
      quiz.questions.each do |question|
        perr = question.parse_errors(:text)
        ierr = question.image_errors(:text)
        ref = %(<a href="/chapters/#{chapter.to_param}/chapter_quiz/chapter_questions_sets/#{question.quizzable.to_param}/chapter_quiz_questions/#{question.to_param}/edit">#{question.reference}</a>).html_safe
        @errors[chapter.title][:html][ref]   = perr if ref && perr.any?
        @errors[chapter.title][:images][ref] = ierr if ref && ierr.any?
      end

      chapter.sections.each do |section|
        section.subsections.each do |subsection|
          perr = subsection.parse_errors(:text)
          ierr = subsection.image_errors(:text)
          ref = %(<a href="/chapters/#{subsection.section.chapter.to_param}/sections/#{subsection.section.to_param}/subsections">#{subsection.reference}</a>).html_safe
          @errors[chapter.title][:html][ref]   = perr if ref && perr.any?
          @errors[chapter.title][:images][ref] = ierr if ref && ierr.any?
        end

        section.reflections.each do |subsection|
          perr = subsection.parse_errors(:content) + subsection.parse_errors(:answer)
          ierr = subsection.image_errors(:content) + subsection.image_errors(:answer)
          ref = %(<a href="/chapters/#{subsection.section.chapter.to_param}/sections/#{subsection.section.to_param}/subsections">#{subsection.reference}</a>).html_safe
          @errors[chapter.title][:html][ref]   = perr if ref && perr.any?
          @errors[chapter.title][:images][ref] = ierr if ref && ierr.any?
        end

        section.extra_examples.each do |subsection|
          perr = subsection.parse_errors(:content)
          ierr = subsection.image_errors(:content)
          ref = %(<a href="/chapters/#{subsection.section.chapter.to_param}/sections/#{subsection.section.to_param}/subsections">#{subsection.reference}</a>).html_safe
          @errors[chapter.title][:html][ref]   = perr if ref && perr.any?
          @errors[chapter.title][:images][ref] = ierr if ref && ierr.any?
        end

        section.questions.each do |question|
          perr = question.parse_errors(:text)
          ierr = question.image_errors(:text)
          if question.quizzable.is_a?(EntryQuiz)
            ref = %(<a href="/entry_quiz/entry_quiz_questions/#{question.to_param}/edit">#{question.reference}</a>).html_safe
          elsif question.quizzable.is_a?(Section)
            ref = %(<a href="/chapters/#{question.quizzable.chapter.to_param}/sections/#{question.quizzable.to_param}/questions/#{question.to_param}/edit">#{question.reference}</a>).html_safe
          elsif question.quizzable.is_a?(ChapterQuestionsSet)
            ref = %(<a href="/chapters/#{question.quizzable.chapter_quiz.chapter.to_param}/chapter_quiz/chapter_questions_sets/#{question.quizzable.to_param}/chapter_quiz_questions/#{item.to_param}/edit">#{question.reference}</a>).html_safe
          end
          @errors[chapter.title][:html][ref]   = perr if ref && perr.any?
          @errors[chapter.title][:images][ref] = ierr if ref && ierr.any?
        end
      end
    end
  end

  def set_breadcrumb
    @crumbs = [{name: Course.current.name, url: root_path}] if Course.current
  end
end
