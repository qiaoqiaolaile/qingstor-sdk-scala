SHELL := /bin/bash

CROSS_SCALA_VERSIONS=2.12.2 2.11.0

.PHONY: help
help:
	@echo "Please use \`make <target>\` where <target> is one of"
	@echo "  all               to update, generate, unit, test and release this SDK"
	@echo "  update            to update git submodules"
	@echo "  generate          to generate service code"
	@echo "  unit              to run all sort of unit tests except runtime"
	@echo "  test              to run service test"
	@echo "  build_jar         to build and release current version"

.PHONY: all
all: update generate unit test release

.PHONY: update
update:
	git submodule init
	git submodule update --remote
	@echo "ok"

.PHONY: generate
generate:
	@if [[ ! -f "$$(which snips)" ]]; then \
        echo "ERROR: Command \"snips\" not found."; \
    fi
	snips \
        -f ./specs/qingstor/2016-01-06/swagger/api_v2.0.json \
        -t ./template \
        -o ./src/main/scala/com/qingstor/sdk/service
	@rm ./src/main/scala/com/qingstor/sdk/service/Object.scala
	sbt scalafmt
	@echo "OK"

.PHONY: unit
unit:
	@echo "run unit test"
	@sbt clean
	@sbt +test
	@echo "ok"

.PHONY: test
test:
	@echo "run service test"
	@sbt clean
	@for version in ${CROSS_SCALA_VERSIONS}; \
	do \
		sbt """; set scalaVersion := \"$$version\"; test:runMain org.junit.runner.JUnitCore com.qingstor.sdk.steps.TestRunner"""; \
	done
	@echo "ok"

.PHONY: build_jar
build_jar:
	@echo "pack the source code"
	@sbt clean
	@sbt +assembly
	@echo "ok"

.PHONY: clean
clean:
	@sbt +clean
	rm -rf release/
	@echo "ok"
