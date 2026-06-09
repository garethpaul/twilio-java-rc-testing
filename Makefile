.PHONY: build check lint test verify

MVN ?= mvn

lint:
	$(MVN) -q -DskipTests compile

test:
	$(MVN) -q test

build:
	$(MVN) -q -DskipTests package

verify: lint test build

check: verify
	scripts/check-baseline.sh
