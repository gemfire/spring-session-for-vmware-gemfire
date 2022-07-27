#!/usr/bin/env bash
set -ex

BASE_DIR=$(pwd)

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPTDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
SCRIPTDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"


source ${BASE_DIR}/concourse-metadata-resource/concourse_metadata
source ${SCRIPTDIR}/shared_utilities.sh

BUILDROOT=$(pwd)
DEST_DIR=${BUILDROOT}/spring-boot-data-gemfire-results

if [[ -z "${GRADLE_TASK}" ]]; then
  echo "GRADLE_TASK must be set. exiting..."
  exit 1
fi

if [[ -z "${ARTIFACT_SLUG}" ]]; then
  echo "ARTIFACT_SLUG must be set. exiting..."
  exit 1
fi

SANITIZED_GRADLE_TASK=${GRADLE_TASK##*:}
TMPDIR=${DEST_DIR}/tmp
SPRING_DATA_GEMFIRE_BUILD=${DEST_DIR}/spring-boot-data-gemfire
BUILD_TIMESTAMP=$(date +%s)


SPRING_DATA_GEMFIRE_BUILD_VERSION_FILE=${BUILDROOT}/spring-boot-data-gemfire-build-version/number

if [ ! -e "${SPRING_DATA_GEMFIRE_BUILD_VERSION_FILE}" ]; then
  echo "${SPRING_DATA_GEMFIRE_BUILD_VERSION_FILE} file does not exist. Concourse is probably not configured correctly."
  exit 1
fi
if [ -z ${MAINTENANCE_VERSION+x} ]; then
  echo "MAINTENANCE_VERSION is unset. Check your pipeline configuration and make sure this script is called properly."
  exit 1
fi


CONCOURSE_VERSION=$(cat ${SPRING_DATA_GEMFIRE_BUILD_VERSION_FILE})
echo "Concourse VERSION is ${CONCOURSE_VERSION}"
# Rebuild version, zero-padded
FULL_PRODUCT_VERSION=$(get-full-version ${CONCOURSE_VERSION})


directories_file=${DEST_DIR}/artifact_directories
mkdir -p ${TMPDIR}

echo "TMPDIR = ${TMPDIR}"
echo "GRADLE_TASK = ${GRADLE_TASK}"
echo "ARTIFACT_SLUG = ${ARTIFACT_SLUG}"

gcloud config set account ${SERVICE_ACCOUNT}


FILENAME=${ARTIFACT_SLUG}-${FULL_PRODUCT_VERSION}.tgz

pushd ${SPRING_DATA_GEMFIRE_BUILD}
  find-here-test-reports ${directories_file}
  tar zcf ${DEST_DIR}/${FILENAME} -T ${directories_file}
popd

if [[ "${ARTIFACT_BUCKET}" =~ \. ]]; then
  ARTIFACT_SCHEME="http"
else
  ARTIFACT_SCHEME="gs"
fi

ARTIFACTS_DESTINATION="${ARTIFACT_BUCKET}/builds/${BUILD_PIPELINE_NAME}/${FULL_PRODUCT_VERSION}"
TEST_RESULTS_DESTINATION="${ARTIFACTS_DESTINATION}/test-results/${SANITIZED_GRADLE_TASK}/${BUILD_TIMESTAMP}/"
TEST_ARTIFACTS_DESTINATION="${ARTIFACTS_DESTINATION}/test-artifacts/${BUILD_TIMESTAMP}/"

BUILD_ARTIFACTS_FILENAME=spring-boot-data-gemfire-build-artifacts-${FULL_PRODUCT_VERSION}.tgz
BUILD_ARTIFACTS_DESTINATION="${ARTIFACTS_DESTINATION}/${BUILD_ARTIFACTS_FILENAME}"

if [ -n "${TAR_SPRING_DATA_GEMFIRE_BUILD_ARTIFACTS}" ] ; then
  pushd ${SPRING_DATA_GEMFIRE_BUILD}
    tar zcf ${DEST_DIR}/${BUILD_ARTIFACTS_FILENAME} .
    gsutil -q cp ${DEST_DIR}/${BUILD_ARTIFACTS_FILENAME} gs://${BUILD_ARTIFACTS_DESTINATION}
  popd
fi

if [ ! -f "${SPRING_DATA_GEMFIRE_BUILD}/build/reports/combined/index.html" ]; then
    echo "No tests exist, compile failed."
    mkdir -p ${SPRING_DATA_GEMFIRE_BUILD}/build/reports/combined
    echo "<html><head><title>No Test Results Were Captured</title></head><body><h1>No Test Results Were Captured</h1></body></html>" > ${SPRING_DATA_GEMFIRE_BUILD}/build/reports/combined/index.html
fi

pushd ${SPRING_DATA_GEMFIRE_BUILD}/build/reports/combined
  gsutil -q -m cp -r * gs://${TEST_RESULTS_DESTINATION}
popd

API_CHECK_REPORT=$(ls ${SPRING_DATA_GEMFIRE_BUILD}/spring-boot-data-gemfire-assembly/build/reports/rich-report-japi*.html)
if [ -n "${API_CHECK_REPORT}" ]; then
  gsutil -q cp ${API_CHECK_REPORT} gs://${TEST_RESULTS_DESTINATION}api_check_report.html
fi

gsutil cp ${DEST_DIR}/${FILENAME} gs://${TEST_ARTIFACTS_DESTINATION}

set +x


echo ""
printf "\033[92m=-=-=-=-=-=-=-=-=-=-=-=-=-=-=  Test Results URI =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\033[0m\n"
printf "\033[92m${ARTIFACT_SCHEME}://${TEST_RESULTS_DESTINATION}\033[0m\n"
printf "\033[92m=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\033[0m\n"
printf "\n"

if [ -n "${API_CHECK_REPORT}" ]; then
  printf "\033[92m=-=-=-=-=-=-=-=-=-=-=-=-=-=  API Check Results URI -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\033[0m\n"
  printf "\033[92m${ARTIFACT_SCHEME}://${TEST_RESULTS_DESTINATION}api_check_report.html\033[0m\n"
  printf "\033[92m=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=\033[0m\n"
fi

printf "\033[92mTest report artifacts from this job are available at:\033[0m\n"
printf "\n"
printf "\033[92m${ARTIFACT_SCHEME}://${TEST_ARTIFACTS_DESTINATION}${FILENAME}\033[0m\n"

if [ -n "${TAR_SPRING_DATA_GEMFIRE_BUILD_ARTIFACTS}" ] ; then
  printf "\033[92mBuild artifacts from this job are available at:\033[0m\n"
  printf "\n"
  printf "\033[92m${ARTIFACT_SCHEME}://${BUILD_ARTIFACTS_DESTINATION}\033[0m\n"
fi
