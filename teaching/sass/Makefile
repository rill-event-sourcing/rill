.PHONY: teaching
LEARNING_CSS_DIR := ../../learning/generated/learning/public/css
TEACHING_CSS_DIR := ../../teaching/generated/teaching/public/css
LOGIN_CSS_DIR := ../../login/resources/login/public

all: learning teaching login

install:
	bundle install

clean: install
	bundle exec compass clean --css-dir ${LEARNING_CSS_DIR}
	bundle exec compass clean --css-dir ${TEACHING_CSS_DIR}
	bundle exec compass clean --css-dir ${LOGIN_CSS_DIR}

learning: install
	bundle exec compass compile --css-dir ${LEARNING_CSS_DIR}

teaching: install
	bundle exec compass compile --css-dir ${TEACHING_CSS_DIR}

login: install
	bundle exec compass compile --css-dir ${LOGIN_CSS_DIR}

watch-learning: install
	bundle exec compass watch --css-dir ${LEARNING_CSS_DIR}

watch-teaching: install
	bundle exec compass watch --css-dir ${TEACHING_CSS_DIR}

watch-login: install
	bundle exec compass watch --css-dir ${LOGIN_CSS_DIR}
