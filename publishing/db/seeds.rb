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
