.DEFAULT_GOAL := check
.PHONY: __repository-make-authority build check lint root-test test verify

PUBLIC_TARGETS := build check lint root-test test verify

MVN ?= mvn
override MVN := $(value MVN)
export MVN
override REPOSITORY_MAKE_DOLLAR := $$
override REPOSITORY_MAKE_OPEN := (
ifneq ($(findstring $(REPOSITORY_MAKE_DOLLAR)$(REPOSITORY_MAKE_OPEN),$(value MVN)),)
$(error MVN must be literal command text, not Make syntax)
endif
override SHELL := /bin/sh
override .SHELLFLAGS := -c

ifneq ($(filter command line,$(origin MAKEFLAGS)),)
$(error MAKEFLAGS must not be overridden for repository verification)
endif
override REPOSITORY_MAKE_FIRST_FLAGS := $(firstword $(MAKEFLAGS))
ifneq ($(filter -%,$(REPOSITORY_MAKE_FIRST_FLAGS)),)
override REPOSITORY_MAKE_FIRST_FLAGS :=
endif
override REPOSITORY_MAKE_SHORT_FLAGS := $(REPOSITORY_MAKE_FIRST_FLAGS) $(filter-out --%,$(filter -%,$(MAKEFLAGS)))
ifneq ($(findstring n,$(REPOSITORY_MAKE_SHORT_FLAGS)),)
$(error non-executing or error-ignoring MAKEFLAGS are not supported for repository verification)
endif
ifneq ($(findstring t,$(REPOSITORY_MAKE_SHORT_FLAGS)),)
$(error non-executing or error-ignoring MAKEFLAGS are not supported for repository verification)
endif
ifneq ($(findstring q,$(REPOSITORY_MAKE_SHORT_FLAGS)),)
$(error non-executing or error-ignoring MAKEFLAGS are not supported for repository verification)
endif
ifneq ($(findstring i,$(REPOSITORY_MAKE_SHORT_FLAGS)),)
$(error non-executing or error-ignoring MAKEFLAGS are not supported for repository verification)
endif
ifneq ($(filter --just-print --dry-run --recon --touch --question --ignore-errors,$(MAKEFLAGS)),)
$(error non-executing or error-ignoring MAKEFLAGS are not supported for repository verification)
endif
ifneq ($(strip $(MAKEFILES)),)
$(error MAKEFILES must be empty; repository verification requires this Makefile to be loaded alone)
endif
override MAKEFILES :=
ifneq ($(origin MAKEFILE_LIST),file)
$(error MAKEFILE_LIST must not be overridden)
endif
override REPOSITORY_MAKEFILE := $(lastword $(MAKEFILE_LIST))
override REPOSITORY_ROOT := $(abspath $(dir $(REPOSITORY_MAKEFILE)))
override ROOT := $(REPOSITORY_ROOT)
export ROOT

$(PUBLIC_TARGETS): override SHELL := /bin/sh
$(PUBLIC_TARGETS): override .SHELLFLAGS := -c
$(PUBLIC_TARGETS): override ROOT := $(REPOSITORY_ROOT)
$(PUBLIC_TARGETS): __repository-make-authority

__repository-make-authority:
	@:

lint:
	cd "$$ROOT" && $$MVN -q -DskipTests compile

test:
	cd "$$ROOT" && $$MVN -q test

build:
	cd "$$ROOT" && $$MVN -q -DskipTests package

verify: lint test build

root-test:
	/bin/sh "$$ROOT/scripts/test-makefile-authority.sh"
	/bin/sh "$$ROOT/scripts/test-workflow-authority.sh"

check: root-test verify
	"$$ROOT/scripts/check-baseline.sh"
