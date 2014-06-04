# Basic Layout with two courses

math = Course.create(name: 'Math')
english = Course.create(name: 'English')

# For each course we put two chapters

## Math
algebra = Chapter.create(title: 'Algebra', description: 'A bunch of abstract things', course: math)
geometry = Chapter.create(title: 'Geometry', description: 'A bunch of weird shapes', course: math)

## English
grammar = Chapter.create(title: 'Grammar', description: 'A bunch of strict rules', course: english)
literature = Chapter.create(title: 'Literature', description: 'A bunch of words', course: english)
