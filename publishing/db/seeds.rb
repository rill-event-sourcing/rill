# Basic Layout with two courses

math = Course.create(name: 'Counting')
english = Course.create(name: 'English')

# Two chapters

## Counting
positional_system = Chapter.create(title: 'Positional system', description: 'Position matters', course: math)
basic_operations = Chapter.create(title: 'Basic operations', description: 'Back to basics', course: math)


# For each chapter, we put two sections

## Position system

grouping_by_10 = Section.create(title: 'Grouping by 10', description: '10 by 10', chapter: positional_system)
position_of_0 = Section.create(title: 'Position of the 0', description: 'at the back or at the front?', chapter: positional_system)

## Basic operations
addition = Section.create(title: 'Addition', description: '2+2=5 for the majority of values of 2', chapter: basic_operations)
subtraction = Section.create(title: 'Subtraction', description: 'Too many things, let me get rid of some', chapter: basic_operations)


## For each section, we put three subsections

counting_to_10 = Subsection.create(title: 'Counting to 10', text: '1+1+1+... = 10', stars: 1, section: grouping_by_10)
summing_up_groups = Subsection.create(title: 'Summing up the number of the groups', text: 'Keep adding!', stars: 2, section: grouping_by_10)
adding_up_total_and_remainder = Subsection.create(title: 'Adding up everything with the remainder', text: 'Something did not fit in groups...', stars: 1, section: grouping_by_10)

zero_to_the_left = Subsection.create(title: '0 to the left', text: 'It does not matter!', stars: 1, section: position_of_0)
zero_to_the_right = Subsection.create(title: '0 to the right', text: 'Hey, I multiply by 10!', stars: 2, section: position_of_0)
zero_to_the_center = Subsection.create(title: '0 in the center', text: 'What now?', stars: 2, section: position_of_0)

adding_small_numbers = Subsection.create(title: 'Adding small numbers', text: 'This looks easy', stars: 1, section: addition)
adding_medium_numbers = Subsection.create(title: 'Adding medium numbers', text: 'This looks average', stars: 2, section: addition)
adding_large_numbers = Subsection.create(title: 'Adding large numbers', text: 'This looks hard!', stars: 3, section: addition)


subtracting_small_numbers = Subsection.create(title: 'Subtracting small numbers', text: 'This looks easy', stars: 1, section: subtraction)
subtracting_large_numbers = Subsection.create(title: 'Subtracting medium numbers', text: 'This looks hard enough already', stars: 2, section: subtraction)
going_negative = Subsection.create(title: 'Going negative', text: 'This looks weird', stars: 3, section: subtraction)

# more to work with

counting_to_100 = Subsection.create(title: 'Counting to 100', text: '1+1+1+... = 100', stars: 3, section: grouping_by_10)
summing_up_in_big_groups = Subsection.create(title: 'Summing up in big groups', text: 'Keep adding!', stars: 2, section: grouping_by_10)
adding_up_again = Subsection.create(title: 'Adding up everything again', text: 'Moar additions', stars: 2, section: grouping_by_10)

one_to_the_left = Subsection.create(title: '1 to the left', text: 'It does not matter!', stars: 1, section: position_of_0)
one_to_the_right = Subsection.create(title: '1 to the right', text: 'Hey, I multiply by 10!', stars: 1, section: position_of_0)
one_to_the_center = Subsection.create(title: '1 in the center', text: 'What now?', stars: 2, section: position_of_0)

adding_smallish_numbers = Subsection.create(title: 'Adding smallish numbers', text: 'This looks easy', stars: 1, section: addition)
adding_mediumish_numbers = Subsection.create(title: 'Adding mediumish numbers', text: 'This looks average', stars: 3, section: addition)
adding_largish_numbers = Subsection.create(title: 'Adding largish numbers', text: 'This looks hard!', stars: 3, section: addition)


subtracting_smallish_numbers = Subsection.create(title: 'Subtracting smallish numbers', text: 'This looks easy', stars: 1, section: subtraction)
subtracting_largish_numbers = Subsection.create(title: 'Subtracting mediumish numbers', text: 'This looks hard enough already', stars: 3, section: subtraction)
going_negativish = Subsection.create(title: 'Going negativish', text: 'This looks weird', stars: 1, section: subtraction)


## Add some questions and answers

grouping_question_1 = Question.create(text: "Grouping question 1 text", section: grouping_by_10)
grouping_question_1_line_input = LineInput.create(question: grouping_question_1)
grouping_question_1_line_input_answer = Answer.create(value: "Grouping Question 1 Line input answer value", line_input: grouping_question_1_line_input)

grouping_question_2 = Question.create(text: "Grouping question 2 text", section: grouping_by_10)
grouping_question_2_line_input = LineInput.create(question: grouping_question_2)
grouping_question_2_line_input_answer = Answer.create(value: "Grouping Question 2 Line input answer value", line_input: grouping_question_2_line_input)

grouping_question_3 = Question.create(text: "Grouping question 3 text", section: grouping_by_10)
grouping_question_3_line_input = LineInput.create(question: grouping_question_3)
grouping_question_3_line_input_answer = Answer.create(value: "Grouping Question 3 Line input answer value", line_input: grouping_question_3_line_input)
