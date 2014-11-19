# Basic Layout with two courses

counting = Course.create(name: 'Counting')
english = Course.create(name: 'English')

# One entry quiz
entry_quiz_counting = EntryQuiz.create(instructions: "Do this and that", feedback: "Great, you're done with the entry quiz", threshold: 1, course: counting)
# With two questions
eq_1 = Question.create(text: "2+2= _INPUT_1_ Which one is highest? _INPUT_2_", active: true, worked_out_answer: "4 and 42", quizzable: entry_quiz_counting)
eq_1_1 = LineInput.create(inputable: eq_1, prefix: "", suffix: "", style: "small")
eq_1_1_answer = Answer.create(value: "4", line_input: eq_1_1)
eq_1_2 = MultipleChoiceInput.create(inputable: eq_1)
eq_1_2_1 = Choice.create(multiple_choice_input: eq_1_2, value: "1", correct: false)
eq_1_2_2 = Choice.create(multiple_choice_input: eq_1_2, value: "16", correct: false)
eq_1_2_3 = Choice.create(multiple_choice_input: eq_1_2, value: "42", correct: true)

eq_2 = Question.create(text: "One banana plus two bananas? _INPUT_2_ What is the lowest natural number? _INPUT_1_", active: true, worked_out_answer: "three bananas! 0", quizzable: entry_quiz_counting)
eq_2_1 = LineInput.create(inputable: eq_2, suffix: "bananas", width: 200, style: "exponent")
eq_2_1_answer = Answer.create(value: "3", line_input: eq_2_1)
eq_2_1_answer_2  = Answer.create(value: "three", line_input: eq_2_1)

eq_2_2  = MultipleChoiceInput.create(inputable: eq_2)
eq_2_2_1  = Choice.create(multiple_choice_input: eq_2_2, value: "1", correct: false)
eq_2_2_2  = Choice.create(multiple_choice_input: eq_2_2, value: "0", correct: true)
eq_2_2_3  = Choice.create(multiple_choice_input: eq_2_2, value: "-1", correct: false)

# Two chapters

## Counting
positional_system = Chapter.create(title: 'Positional system', active:true, description: 'Position matters', course: counting, remedial: false)
basic_operations = Chapter.create(title: 'Basic operations', description: 'Back to basics', course: counting)


# For each chapter, we put two sections

## Position system

grouping_by_10 = Section.create(title: 'Grouping by 10', active: true, description: '10 by 10', chapter: positional_system, meijerink_criteria: ["1F-RT"], domains: ["Getallen"])
position_of_0 = Section.create(title: 'Position of the 0', active: true, description: 'at the back or at the front?', chapter: positional_system, meijerink_criteria: ["1F-RT", "2F","3F"], domains: ["Getallen"])

## Basic operations
addition = Section.create(title: 'Addition', description: '2+2=5 for the majority of values of 2', chapter: basic_operations, meijerink_criteria: ["1F-RT", "2F"], domains: ["Getallen", "Verbanden"])
subtraction = Section.create(title: 'Subtraction', description: 'Too many things, let me get rid of some', chapter: basic_operations, meijerink_criteria: ["1F-RT", "2F", "3F"], domains: ["Getallen", "Verhoudingen", "Meetkunde", "Verbanden"])


## We have some reflections

reflection1 = Reflection.create(content: '1+1=', answer: '2', section: grouping_by_10)
reflection2 = Reflection.create(content: '2+2=', answer: '4', section: grouping_by_10)
reflecting = Subsection.create(title: 'Reflecting', text: 'reflections _REFLECTION_1_ <br> _REFLECTION_2_', section: grouping_by_10)

## We have some extra examples

extra_example1 = ExtraExample.create(title: 'extra example', default_open: true, content: 'some extra exampling', section: grouping_by_10)
extra_example2 = ExtraExample.create(title: 'extra example too', content: 'a lot of extra exampling', section: grouping_by_10)
reflecting = Subsection.create(title: 'Extra examples', text: 'extra examples _EXTRA_EXAMPLE_1_ <br> _EXTRA_EXAMPLE_2_', section: grouping_by_10)

## For each section, we put three subsections

counting_to_10 = Subsection.create(title: 'Counting to 10', text: '1+1+1+... = 10', section: grouping_by_10)
summing_up_groups = Subsection.create(title: 'Summing up the number of the groups', text: 'Keep adding!', section: grouping_by_10)
adding_up_total_and_remainder = Subsection.create(title: 'Adding up everything with the remainder', text: 'Something did not fit in groups...', section: grouping_by_10)
counting_to_100 = Subsection.create(title: 'Counting to 100', text: '1+1+1+... = 100', section: grouping_by_10)
summing_up_in_big_groups = Subsection.create(title: 'Summing up in big groups', text: 'Keep Adding! _INPUT_1_', section: grouping_by_10)
adding_up_again = Subsection.create(title: 'Adding up everything again', text: 'Moar additions', section: grouping_by_10)
grouping_by_10_line_input = LineInput.create(inputable: grouping_by_10, prefix: "666", suffix: "euros")
grouping_by_10_line_input_answer = Answer.create(value: "123", line_input: grouping_by_10_line_input)

zero_to_the_left = Subsection.create(title: '0 to the left', text: 'It does not matter!', section: position_of_0)
zero_to_the_right = Subsection.create(title: '0 to the right', text: 'Hey, I multiply by 10!', section: position_of_0)
zero_to_the_center = Subsection.create(title: '0 in the center', text: 'What now?', section: position_of_0)
one_to_the_left = Subsection.create(title: '1 to the left', text: 'It does not matter!', section: position_of_0)
one_to_the_right = Subsection.create(title: '1 to the right', text: 'Hey, I multiply by 10!', section: position_of_0)
one_to_the_center = Subsection.create(title: '1 in the center', text: 'What now?', section: position_of_0)


adding_small_numbers = Subsection.create(title: 'Adding small numbers', text: 'This looks easy', section: addition)
adding_medium_numbers = Subsection.create(title: 'Adding medium numbers', text: 'This looks average', section: addition)
adding_large_numbers = Subsection.create(title: 'Adding large numbers', text: 'This looks hard!', section: addition)
adding_smallish_numbers = Subsection.create(title: 'Adding smallish numbers', text: 'This looks easy', section: addition)
adding_mediumish_numbers = Subsection.create(title: 'Adding mediumish numbers', text: 'This looks average', section: addition)
adding_largish_numbers = Subsection.create(title: 'Adding largish numbers', text: 'This looks hard!', section: addition)


subtracting_small_numbers = Subsection.create(title: 'Subtracting small numbers', text: 'This looks easy', section: subtraction)
subtracting_large_numbers = Subsection.create(title: 'Subtracting medium numbers', text: 'This looks hard enough already', section: subtraction)
going_negative = Subsection.create(title: 'Going negative', text: 'This looks weird', section: subtraction)
subtracting_smallish_numbers = Subsection.create(title: 'Subtracting smallish numbers', text: 'This looks easy', section: subtraction)
subtracting_largish_numbers = Subsection.create(title: 'Subtracting mediumish numbers', text: 'This looks hard enough already', section: subtraction)
going_negativish = Subsection.create(title: 'Going negativish', text: 'This looks weird', section: subtraction)


## Add some questions and answers for subsections

grouping_question_1 = Question.create(text: "Grouping question 1 text _INPUT_1_ and of course _INPUT_2_", active: true, worked_out_answer: "Something something here", quizzable: grouping_by_10)
grouping_question_1_line_input = LineInput.create(inputable: grouping_question_1, prefix: "pre", suffix: "suf")
grouping_question_1_line_input_answer = Answer.create(value: "42", line_input: grouping_question_1_line_input)
grouping_question_1_multiple_choice_input = MultipleChoiceInput.create(inputable: grouping_question_1)
grouping_question_1_multiple_choice_input_choice1 = Choice.create(multiple_choice_input: grouping_question_1_multiple_choice_input, value: "1", correct: false)
grouping_question_1_multiple_choice_input_choice2 = Choice.create(multiple_choice_input: grouping_question_1_multiple_choice_input, value: "2", correct: false)
grouping_question_1_multiple_choice_input_choice3 = Choice.create(multiple_choice_input: grouping_question_1_multiple_choice_input, value: "3", correct: true)

grouping_question_2 = Question.create(text: "And now, for something completely different _INPUT_2_ and _INPUT_1_", active: true, worked_out_answer: "This is supposed to explain", quizzable: grouping_by_10)
grouping_question_2_line_input = LineInput.create(inputable: grouping_question_2, suffix: "ohlala")
grouping_question_2_line_input_answer = Answer.create(value: "6", line_input: grouping_question_2_line_input)
grouping_question_2_line_input_answer2 = Answer.create(value: "8", line_input: grouping_question_2_line_input)

grouping_question_2_multiple_choice_input = MultipleChoiceInput.create(inputable: grouping_question_2)
grouping_question_2_multiple_choice_input_choice1 = Choice.create(multiple_choice_input: grouping_question_2_multiple_choice_input, value: "123", correct: false)
grouping_question_2_multiple_choice_input_choice2 = Choice.create(multiple_choice_input: grouping_question_2_multiple_choice_input, value: "nsadot really", correct: false)
grouping_question_2_multiple_choice_input_choice3 = Choice.create(multiple_choice_input: grouping_question_2_multiple_choice_input, value: "correct", correct: true)

pos0q1 = Question.create(text: "1 + 1 _INPUT_1_", worked_out_answer: "2", quizzable: position_of_0)
pos0q1i = LineInput.create(inputable: pos0q1, prefix: "pre", suffix: "suf")
pos0q1ia = Answer.create(value: "2", line_input: pos0q1i)

### Chapter quiz

ch1_quiz = ChapterQuiz.create(chapter: positional_system, active: true)

ch1_quiz_set1 = ChapterQuestionsSet.create(title: "QS1", chapter_quiz: ch1_quiz)

ch1_s1_1 = Question.create(text: "2 _INPUT_1_ 2 _INPUT_2_", active: true, worked_out_answer: "2 and 2", quizzable: ch1_quiz_set1)
ch1_s1_1_1 = LineInput.create(inputable: ch1_s1_1, prefix: "", suffix: "")
ch1_s1_1_1_answer = Answer.create(value: "2", line_input: ch1_s1_1_1)
ch1_s1_1_2 = MultipleChoiceInput.create(inputable: ch1_s1_1)
ch1_s1_1_2_1 = Choice.create(multiple_choice_input: ch1_s1_1_2, value: "1", correct: false)
ch1_s1_1_2_2 = Choice.create(multiple_choice_input: ch1_s1_1_2, value: "0", correct: false)
ch1_s1_1_2_3 = Choice.create(multiple_choice_input: ch1_s1_1_2, value: "2", correct: true)

ch1_s1_2 = Question.create(text: "7 _INPUT_2_ 2 _INPUT_1_", active: true, worked_out_answer: "7 2", quizzable: ch1_quiz_set1)
ch1_s1_2_1 = LineInput.create(inputable: ch1_s1_2, suffix: "")
ch1_s1_2_1_answer = Answer.create(value: "7", line_input: ch1_s1_2_1)
ch1_s1_2_1_answer_2  = Answer.create(value: "seven", line_input: ch1_s1_2_1)

ch1_s1_2_2  = MultipleChoiceInput.create(inputable: ch1_s1_2)
ch1_s1_2_2_1  = Choice.create(multiple_choice_input: ch1_s1_2_2, value: "1", correct: false)
ch1_s1_2_2_2  = Choice.create(multiple_choice_input: ch1_s1_2_2, value: "2", correct: true)
ch1_s1_2_2_3  = Choice.create(multiple_choice_input: ch1_s1_2_2, value: "-1", correct: false)

##
ch1_quiz_set2 = ChapterQuestionsSet.create(title: "Question set 2", chapter_quiz: ch1_quiz)

ch1_s2_1 = Question.create(text: "6 _INPUT_1_ 100 _INPUT_2_", active: true, worked_out_answer: "6 and 100", quizzable: ch1_quiz_set2)
ch1_s2_1_1 = LineInput.create(inputable: ch1_s2_1, prefix: "", suffix: "")
ch1_s2_1_1_answer = Answer.create(value: "6", line_input: ch1_s2_1_1)
ch1_s2_1_2 = MultipleChoiceInput.create(inputable: ch1_s2_1)
ch1_s2_1_2_1 = Choice.create(multiple_choice_input: ch1_s2_1_2, value: "100", correct: true)
ch1_s2_1_2_2 = Choice.create(multiple_choice_input: ch1_s2_1_2, value: "16", correct: false)
ch1_s2_1_2_3 = Choice.create(multiple_choice_input: ch1_s2_1_2, value: "42", correct: false)

ch1_s2_2 = Question.create(text: "3 three _INPUT_2_ 1 _INPUT_1_", active: true, worked_out_answer: "three kiwis! 1", quizzable: ch1_quiz_set2)
ch1_s2_2_1 = LineInput.create(inputable: ch1_s2_2, suffix: "")
ch1_s2_2_1_answer = Answer.create(value: "3", line_input: ch1_s2_2_1)
ch1_s2_2_1_answer_2  = Answer.create(value: "three", line_input: ch1_s2_2_1)

ch1_s2_2_2  = MultipleChoiceInput.create(inputable: ch1_s2_2)
ch1_s2_2_2_1  = Choice.create(multiple_choice_input: ch1_s2_2_2, value: "1", correct: true)
ch1_s2_2_2_2  = Choice.create(multiple_choice_input: ch1_s2_2_2, value: "0", correct: false)
ch1_s2_2_2_3  = Choice.create(multiple_choice_input: ch1_s2_2_2, value: "-1", correct: false)

#
ch1_quiz_set3 = ChapterQuestionsSet.create(title: "Question set 3", chapter_quiz: ch1_quiz)

ch1_s3_1 = Question.create(text: "12 _INPUT_1_ 10 _INPUT_2_", active: true, worked_out_answer: "12 and 10", quizzable: ch1_quiz_set3)
ch1_s3_1_1 = LineInput.create(inputable: ch1_s3_1, prefix: "", suffix: "")
ch1_s3_1_1_answer = Answer.create(value: "12", line_input: ch1_s3_1_1)
ch1_s3_1_2 = MultipleChoiceInput.create(inputable: ch1_s3_1)
ch1_s3_1_2_1 = Choice.create(multiple_choice_input: ch1_s3_1_2, value: "100", correct: false)
ch1_s3_1_2_2 = Choice.create(multiple_choice_input: ch1_s3_1_2, value: "10", correct: true)
ch1_s3_1_2_3 = Choice.create(multiple_choice_input: ch1_s3_1_2, value: "42", correct: false)

ch1_s3_2 = Question.create(text: "4 four _INPUT_2_ 123 _INPUT_1_", active: true, worked_out_answer: "4 123", quizzable: ch1_quiz_set3)
ch1_s3_2_1 = LineInput.create(inputable: ch1_s3_2, suffix: "")
ch1_s3_2_1_answer = Answer.create(value: "4", line_input: ch1_s3_2_1)
ch1_s3_2_1_answer_2  = Answer.create(value: "four", line_input: ch1_s3_2_1)

ch1_s3_2_2  = MultipleChoiceInput.create(inputable: ch1_s3_2)
ch1_s3_2_2_1  = Choice.create(multiple_choice_input: ch1_s3_2_2, value: "1", correct: false)
ch1_s3_2_2_2  = Choice.create(multiple_choice_input: ch1_s3_2_2, value: "123", correct: true)
ch1_s3_2_2_3  = Choice.create(multiple_choice_input: ch1_s3_2_2, value: "-1", correct: false)

#
ch1_quiz_set4 = ChapterQuestionsSet.create(title: "Qs4", chapter_quiz: ch1_quiz)

ch1_s4_1 = Question.create(text: "42 _INPUT_1_ 101 _INPUT_2_", active: true, worked_out_answer: "42 and 101", quizzable: ch1_quiz_set4)
ch1_s4_1_1 = LineInput.create(inputable: ch1_s4_1, prefix: "", suffix: "")
ch1_s4_1_1_answer = Answer.create(value: "42", line_input: ch1_s4_1_1)
ch1_s4_1_2 = MultipleChoiceInput.create(inputable: ch1_s4_1)
ch1_s4_1_2_1 = Choice.create(multiple_choice_input: ch1_s4_1_2, value: "101", correct: true)
ch1_s4_1_2_2 = Choice.create(multiple_choice_input: ch1_s4_1_2, value: "16", correct: false)
ch1_s4_1_2_3 = Choice.create(multiple_choice_input: ch1_s4_1_2, value: "23", correct: false)

ch1_s4_2 = Question.create(text: "321 three _INPUT_2_ 101 _INPUT_1_", active: true, worked_out_answer: "321 101", quizzable: ch1_quiz_set4)
ch1_s4_2_1 = LineInput.create(inputable: ch1_s4_2, suffix: "")
ch1_s4_2_1_answer = Answer.create(value: "321", line_input: ch1_s4_2_1)

ch1_s4_2_2  = MultipleChoiceInput.create(inputable: ch1_s4_2)
ch1_s4_2_2_1  = Choice.create(multiple_choice_input: ch1_s4_2_2, value: "1", correct: false)
ch1_s4_2_2_2  = Choice.create(multiple_choice_input: ch1_s4_2_2, value: "0", correct: false)
ch1_s4_2_2_3  = Choice.create(multiple_choice_input: ch1_s4_2_2, value: "101", correct: true)

