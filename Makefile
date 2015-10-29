all:
	git checkout master doc/rill.org
	org2html doc/rill.org
	mv doc/rill.html ./index.html
	mv doc/*.svg ./
	git rm -rf doc

