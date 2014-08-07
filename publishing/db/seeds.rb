# Basic Layout with two courses

counting = Course.create(name: 'Counting')
english = Course.create(name: 'English')

# Two chapters

## Counting
positional_system = Chapter.create(title: 'Positional system', active:true, description: 'Position matters', course: counting)
basic_operations = Chapter.create(title: 'Basic operations', description: 'Back to basics', course: counting)


# For each chapter, we put two sections

## Position system

grouping_by_10 = Section.create(title: 'Grouping by 10', active: true, description: '10 by 10', chapter: positional_system)
position_of_0 = Section.create(title: 'Position of the 0', active: true, description: 'at the back or at the front?', chapter: positional_system)

## Basic operations
addition = Section.create(title: 'Addition', description: '2+2=5 for the majority of values of 2', chapter: basic_operations)
subtraction = Section.create(title: 'Subtraction', description: 'Too many things, let me get rid of some', chapter: basic_operations)


## For each section, we put three subsections

counting_to_10 = Subsection.create(title: 'Counting to 10', text: '1+1+1+... = 10', section: grouping_by_10)
summing_up_groups = Subsection.create(title: 'Summing up the number of the groups', text: 'Keep adding!', section: grouping_by_10)
adding_up_total_and_remainder = Subsection.create(title: 'Adding up everything with the remainder', text: 'Something did not fit in groups...', section: grouping_by_10)
counting_to_100 = Subsection.create(title: 'Counting to 100', text: '1+1+1+... = 100', section: grouping_by_10)
summing_up_in_big_groups = Subsection.create(title: 'Summing up in big groups _INPUT_1_', text: 'Keep adding!', section: grouping_by_10)
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
grouping_question_1_line_input_answer = Answer.create(value: "Grouping Question 1 Line input answer value", line_input: grouping_question_1_line_input)
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

