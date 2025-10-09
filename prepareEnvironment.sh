#!/bin/bash

################################################################################
# This tool prepares a self-contained SDK environment for metaphactory,
# i.e. it retrieves and copies all necessary files and scripts into the current
# directory.
# Run "./prepareEnvironment.sh -h" to see usage.
################################################################################

METAPHACTORY_DOCKER_IMAGE=metaphacts/metaphactory:5.9.0
TIMESTAMP="$(date +%s)"
CONTAINER_NAME="metaphactory-sdk-${TIMESTAMP}"
TEMP_FOLDER="./tmp/${CONTAINER_NAME}"
SDK_FOLDER="./.sdk"

while getopts "fhi:" option; do
	case $option in
		f) # force mode
			FORCE="yes"
			;;
		h) # help/usage
			echo "Usage: $0 [OPTIONS]"
			echo "OPTIONS:"
			echo "-f  force update of SDK, even if the metaphactory version did not change"
			echo "-h  display this help"
			echo "-i <image>  use custom docker image, default: ${METAPHACTORY_DOCKER_IMAGE}"
			echo "Examples:"
			echo "  - with custom docker image: \"$0 -i ${METAPHACTORY_DOCKER_IMAGE}\""
			exit 0
			;;
		i) # custom image
			METAPHACTORY_DOCKER_IMAGE="${OPTARG}"
			;;
	esac
done

# If force mode is not active check for new version
if [ -z "${FORCE}" ]
then
	if [ -e "./metaphactory-release" ] && [ "$(cat ./metaphactory-release)" == "SDK created from ${METAPHACTORY_DOCKER_IMAGE}" ]
	then
		SAME_VERSION="yes"
	fi

	# Exit script if the SDK folder already exists and there is no new version
	if [ -e "${SDK_FOLDER}" ] && [ -n "${SAME_VERSION}" ]
	then
		echo "SDK folder \"${SDK_FOLDER}\" already exists, skipping SDK setup. To update the SDK, please delete this folder."
		exit 0
	fi
fi

# Create the container temporarily to extract data
docker create --name "${CONTAINER_NAME}" "${METAPHACTORY_DOCKER_IMAGE}" > /dev/null

if [ $? -ne 0 ]
then
	echo "Failed to create metaphactory container"
	exit 1
fi

# Copy SDK from container and unzip it
docker cp "${CONTAINER_NAME}:/sdk/sdk.zip" "./sdk.zip" > /dev/null

if [ $? -ne 0 ]
then
	echo "Failed to extract SDK from metaphactory container"
	docker rm "${CONTAINER_NAME}" > /dev/null
	exit 1
fi

mkdir -p "${TEMP_FOLDER}"
unzip "./sdk.zip" -d "${TEMP_FOLDER}" > /dev/null
cp -rf "${TEMP_FOLDER}/sdk/." .
rm -r "${TEMP_FOLDER}"
rm "./sdk.zip"

echo "SDK created from ${METAPHACTORY_DOCKER_IMAGE}" > metaphactory-release
cat metaphactory-release

# Copy metaphactory WAR file from container
if [ -e "./metaphactory-release.war" ]
then
	rm "./metaphactory-release.war"
fi
docker cp "${CONTAINER_NAME}:/var/lib/jetty/webapps/ROOT.war" "./metaphactory-release.war" > /dev/null

# Remove temporary container
docker rm "${CONTAINER_NAME}" > /dev/null

sed "s/platformLocation=.*/platformLocation=.\/metaphactory-release.war/" gradle.properties.template > gradle.properties
rm gradle.properties.template

./gradlew prepareEnvironment
