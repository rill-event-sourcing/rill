default: test

.PHONY: test

test:
	cd server && make test

import:
	cd server && lein validate-course-material /tmp/material.json
