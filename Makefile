.PHONY: build check lint test verify

ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
MVN ?= mvn

lint:
	cd "$(ROOT)" && $(MVN) -q -DskipTests compile

test:
	cd "$(ROOT)" && $(MVN) -q test

build:
	cd "$(ROOT)" && $(MVN) -q -DskipTests package

verify: lint test build

check: verify
	"$(ROOT)/scripts/check-baseline.sh"
