class CheckingController < ApplicationController

  def index
    @parse_errors = {}
    @image_errors = {}
    [Question, Subsection, Choice].each do |html_type|
      html_type.find_in_batches.each do |items|
        items.each do |item|
          perr = item.parse_errors
          ierr = item.image_errors
          if perr.any? || ierr.any?
            if item.is_a?(Question)
              if item.quizzable.is_a?(EntryQuiz)
                ref = %(<a href="/entry_quiz/entry_quiz_questions/#{item.to_param}/edit">#{item.reference}/edit</a>).html_safe
              elsif item.quizzable.is_a?(Section)
                ref = %(<a href="/chapters/#{item.quizzable.chapter.to_param}/sections/#{item.quizzable.to_param}/questions/#{item.to_param}/edit">#{item.reference}</a>).html_safe
              end
            elsif item.is_a?(Subsection)
              ref = %(<a href="/chapters/#{item.section.chapter.to_param}/sections/#{item.section.to_param}/sections/#{item.section.to_param}/edit">#{item.reference}</a>).html_safe
            elsif item.is_a?(Choice)
              ref = %(<a href="/chapters/#{item.multiple_choice_input.inputable.quizzable.chapter.to_param}/sections/#{item.multiple_choice_input.inputable.quizzable.to_param}/questions/#{item.multiple_choice_input.inputable.to_param}/edit">#{item.reference}</a>).html_safe
            end
          end
          @parse_errors[ref] = perr if ref && perr.any?
          @image_errors[ref] = ierr if ref && ierr.any?
        end
      end
    end
  end
end
