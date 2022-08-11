#!/usr/bin/env bash

find-here-test-reports() {
  output_directories_file=${1}
  set +e
  find . -type d -name "reports" > ${output_directories_file}
  find .  -type d -name "test-results" >> ${output_directories_file}
  (find . -type d -name "*Test" | grep "build/[^/]*Test$") >> ${output_directories_file}
  find . -name "*-progress*txt" >> ${output_directories_file}
  find . -name "*.hprof" -o -name "hs_err*.log" -o -name "replay*.log" >> ${output_directories_file}
  find . -type d -name "callstacks" >> ${output_directories_file}
  find .gradle_logs -name '*.log' >> ${output_directories_file}
  echo "Collecting the following artifacts..."
  cat ${output_directories_file}
  echo ""
}

## Parsing functions for the Concourse Semver resource.
## These functions expect one input in the form of the resource file, e.g., "1.14.0-build.325"
get-spring-session-data-gemfire-version() {
  local CONCOURSE_VERSION=$1
  # Prune all after '-', yielding e.g., "1.14.0"
  local SPRING_DATA_GEMFIRE_PRODUCT_VERSION=${CONCOURSE_VERSION%%-*}
  (>&2 echo "Geode product VERSION is ${SPRING_DATA_GEMFIRE_PRODUCT_VERSION}")
  echo ${SPRING_DATA_GEMFIRE_PRODUCT_VERSION}
}

get-spring-session-data-gemfire-version-qualifier-slug() {
  local CONCOURSE_VERSION=$1
  # Prune all before '-', yielding e.g., "build.325"
  local CONCOURSE_BUILD_SLUG=${CONCOURSE_VERSION##*-}
  # Prune all before '.', yielding e.g., "build"
  local QUALIFIER_SLUG=${CONCOURSE_BUILD_SLUG%%.*}
  echo ${QUALIFIER_SLUG}
}

get-spring-session-data-gemfire-build-id() {
  local CONCOURSE_VERSION=$1
  # Prune all before the last '.', yielding e.g., "325"
  local BUILD_ID=${CONCOURSE_VERSION##*.}
  echo ${BUILD_ID}
}

get-spring-session-data-gemfire-build-id-padded() {
  local CONCOURSE_VERSION=$1
  local BUILD_ID=$(get-spring-session-data-gemfire-build-id ${CONCOURSE_VERSION})
  # Prune all before the last '.', yielding e.g., "325", then zero-pad, e.g., "0325"
  local PADDED_BUILD_ID=$(printf "%04d" ${BUILD_ID})
  (>&2 echo "Build ID is ${PADDED_BUILD_ID}")
  echo ${PADDED_BUILD_ID}
}

get-full-version() {
  # Extract each component so that the BuildId can be zero-padded, then reassembled.
  local CONCOURSE_VERSION=$1
  local SPRING_DATA_GEMFIRE_PRODUCT_VERSION=$(get-spring-session-data-gemfire-version ${CONCOURSE_VERSION})
  local QUALIFIER_SLUG=$(get-spring-session-data-gemfire-version-qualifier-slug ${CONCOURSE_VERSION})
  local PADDED_BUILD_ID=$(get-spring-session-data-gemfire-build-id-padded ${CONCOURSE_VERSION})
  local FULL_PRODUCT_VERSION="${SPRING_DATA_GEMFIRE_PRODUCT_VERSION}-${QUALIFIER_SLUG}.${PADDED_BUILD_ID}"
  (>&2 echo "Full product VERSION is ${FULL_PRODUCT_VERSION}")
  echo ${FULL_PRODUCT_VERSION}
}
