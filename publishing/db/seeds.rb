# Basic Layout with two courses

math = Course.create(name: 'Counting')
english = Course.create(name: 'English')

# Two chapters

## Math
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

counting_to_10 = Subsection.create(title: 'Counting to 10', description: '1+1+1+... = 10', level: 1, section: grouping_by_10)
summing_up_groups = Subsection.create(title: 'Summing up the number of the groups', description: 'Keep adding!', level: 2, section: grouping_by_10)
adding_up_total_and_remainder = Subsection.create(title: 'Adding up everything with the remainder', description: 'Something did not fit in groups...', level: 3, section: grouping_by_10)

zero_to_the_left = Subsection.create(title: '0 to the left', description: 'It does not matter!', level: 1, section: position_of_0)
zero_to_the_right = Subsection.create(title: '0 to the right', description: 'Hey, I multiply by 10!', level: 2, section: position_of_0)
zero_to_the_center = Subsection.create(title: '0 in the center', description: 'What now?', level: 2, section: position_of_0)

adding_small_numbers = Subsection.create(title: 'Adding small numbers', description: 'This looks easy', level: 1, section: addition)
adding_medium_numbers = Subsection.create(title: 'Adding medium numbers', description: 'This looks average', level: 2, section: addition)
adding_large_numbers = Subsection.create(title: 'Adding large numbers', description: 'This looks hard!', level: 3, section: addition)


subtracting_small_numbers = Subsection.create(title: 'Subtracting small numbers', description: 'This looks easy', level: 1, section: subtraction)
subtracting_large_numbers = Subsection.create(title: 'Subtracting medium numbers', description: 'This looks hard enough already', level: 2, section: subtraction)
going_negative = Subsection.create(title: 'Going negative', description: 'This looksw weird', level: 3, section: subtraction)
