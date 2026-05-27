#!/bin/bash

################################################################################
# This tool prepares a self-contained SDK environment for metaphactory,
# i.e. it retrieves and copies all necessary files and scripts into the current
# directory.
# Run "./prepareEnvironment.sh -h" to see usage.
################################################################################

METAPHACTORY_DOCKER_IMAGE=metaphacts/metaphactory:5.11.0
TIMESTAMP="$(date +%s)"
CONTAINER_NAME="metaphactory-sdk-${TIMESTAMP}"
TEMP_FOLDER="./tmp/${CONTAINER_NAME}"
SDK_FOLDER="./.sdk"

while getopts "fhi:a:" option; do
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
			echo "-a <apps>   comma-separated list of bundled apps to copy from the container"
			echo "            into the current directory (e.g., -a ai-services,eia-physical-layer,eia-business-layer,enterprise-information-architecture)"
			echo "Examples:"
			echo "  - with custom docker image: \"$0 -i ${METAPHACTORY_DOCKER_IMAGE}\""
			echo "  - with bundled apps:        \"$0 -a ai-services,eia-physical-layer,eia-business-layer,enterprise-information-architecture\""
			exit 0
			;;
		i) # custom image
			METAPHACTORY_DOCKER_IMAGE="${OPTARG}"
			;;
		a) # bundled apps to download
			BUNDLED_APPS="${OPTARG}"
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

# Copy bundled apps from container if requested
if [ -n "${BUNDLED_APPS}" ]
then
	IFS=',' read -ra APP_LIST <<< "${BUNDLED_APPS}"
	for app in "${APP_LIST[@]}"
	do
		app="$(echo "${app}" | xargs)"
		if [ -z "${app}" ]
		then
			continue
		fi
		if [ -e "./${app}" ]
		then
			rm -rf "./${app}"
		fi
		docker cp "${CONTAINER_NAME}:/bundled/apps/${app}" "./${app}" > /dev/null
		if [ $? -ne 0 ]
		then
			echo "Failed to copy bundled app '${app}' from metaphactory container"
			docker rm "${CONTAINER_NAME}" > /dev/null
			exit 1
		fi
		echo "Downloaded bundled app: ${app}"
	done
fi

# Remove temporary container
docker rm "${CONTAINER_NAME}" > /dev/null

# Only overwrite gradle.properties if it doesn't exist or force mode is active
if [ ! -e "./gradle.properties" ] || [ -n "${FORCE}" ]
then
	SED_ARGS=(-e "s/platformLocation=.*/platformLocation=.\/metaphactory-release.war/")
	if [ -n "${BUNDLED_APPS}" ]
	then
		SED_ARGS+=(-e "s|^#apps=\(.*\)|#apps=${BUNDLED_APPS},\1|")
	fi
	sed "${SED_ARGS[@]}" gradle.properties.template > gradle.properties
	echo "Created/updated gradle.properties from template"
else
	echo "Keeping existing gradle.properties (use -f to force update)"
	if [ -n "${BUNDLED_APPS}" ]
	then
		echo "Note: bundled apps were not added to existing gradle.properties. Use -f or update the 'apps' line manually."
	fi
fi
rm gradle.properties.template

./gradlew prepareEnvironment
