# Commit messages

The better commit messages are, the highest is the chance of finding useful informations without opening the diff.

In general, we follow the best practices outlined in "[On commit messages](http://who-t.blogspot.de/2009/12/on-commit-messages.html)" and "[A note about git commit messages](tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)"

In a nutshell:

1. don't exceed 72 chars per line, we ideally want everything to fit nicely in our terminal;

1. use exhaustive commit messages: the first line is the summary, then an empty line and then a more thorough explanation in a block of text, if needed, separated into paragraphs;

1. **describe the purpose of your change**, in present tense. Visualize commits as actions that are applied one after each other. Don't use the past and describe what you did (e.g. which files you edited), because that can be easily seen by just opening the diff.

  As a rule of thumb, try to be as explicit as possible for commit messages: they provide context for your code changes and can really speed up the reading of the code.

  Incidentally this also means that commits should be divided according to the logic behind it, rather than on a per-file basis and so forth.

1. Don't forget to clean up! Squashing commits is also good and helps the code review and the merging process. Amending is also your friend: whether you forgot to take out a debug command, or to fix a typo, amending or squashing commits is better than having a "_fix typo_" as commit message.

1. Last but not least: if necessary, [ignore the rules](http://en.wikipedia.org/wiki/Wikipedia:Understanding_IAR).
