.PHONY: learning
LEARNING_CSS_DIR := ../../learning/generated/learning/public/css
LOGIN_CSS_DIR := ../../login/resources/login/public

all: learning login

install:
	bundle install

clean: install
	bundle exec compass clean --css-dir ${LEARNING_CSS_DIR}
	bundle exec compass clean --css-dir ${LOGIN_CSS_DIR}

learning: install
	bundle exec compass compile --css-dir ${LEARNING_CSS_DIR}

login: install
	bundle exec compass compile --css-dir ${LOGIN_CSS_DIR}

watch-learning: install
	bundle exec compass watch --css-dir ${LEARNING_CSS_DIR}

watch-login: install
	bundle exec compass watch --css-dir ${LOGIN_CSS_DIR}
