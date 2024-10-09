ARG METAPHACTORY_VERSION=5.5.0
ARG METAPHACTORY_IMAGE=metaphacts/metaphactory
ARG GRADLE_VERSION=8-jdk17

# define metaphactory base image (used below)
FROM ${METAPHACTORY_IMAGE}:${METAPHACTORY_VERSION} AS metaphactory-base

# define app-builder image
FROM gradle:${GRADLE_VERSION} AS app-builder

RUN mkdir /tmp/build \
  && mkdir /tmp/build/installedapps \
  && chown -R 100:0 "/tmp/build/installedapps" \
  && chmod -R g=u "/tmp/build/installedapps" \
  && chmod -R g+ws "/tmp/build/installedapps"

WORKDIR /tmp/build

# copy ROOT.war from metaphactory to extract dependencies
# and libraries required for building Java-based apps
COPY --chown=jetty:jetty --from=metaphactory-base /var/lib/jetty/webapps/ROOT.war /tmp/build/ROOT.war

# copy work dir containing the app project (incl. app build scripts
# taken from sample-apps repo) to the builder image
COPY --chown=jetty:jetty . /tmp/build

RUN echo "platformLocation=/tmp/build/ROOT.war" > /tmp/build/gradle.properties \
  && gradle --info prepareEnvironment

RUN gradle --info clean appZip \
  && echo "apps: " \
  && ls -l /tmp/build/target/apps/ \
  && for app in $(ls -1 /tmp/build/target/apps/*.zip); do \
       echo "extracting app $app to folder $(basename $app .zip)" && unzip "$app" -d "/tmp/build/installedapps/$(basename $app .zip)" ; \
     done \
  && echo "final apps:" \
  && ls -l /tmp/build/installedapps/


FROM metaphactory-base

# copy zipped apps from /tmp/build/target/apps/ in "build" image
COPY --from=app-builder --chown=jetty:root /tmp/build/target/apps/ /zippedapps/
# copy built and extracted apps from /tmp/build/installedapps in "build" image
COPY --from=app-builder --chown=jetty:root /tmp/build/installedapps /installedapps/

# auto-activate each app using an environment variable MP_APP_XXX pointing to the respective app:
# note: this can also be done on-demand when deploying the container,
#       e.g. in a docker-compose file or Kubernetes Pod definition
#ENV MP_APP_1=/installedapps/app1
#ENV MP_APP_2=/installedapps/app2
#ENV MP_APP_3=/installedapps/app3


# to build the container with apps use the following command:
# docker build --build-arg METAPHACTORY_VERSION=5.5.0 -t metaphactory-with-apps:5.5.0 .
# to run the container with one example app use the following command:
# docker run --detach --publish 10214:8080 --env MP_APP_1=/installedapps/eventsubscriber-example --name metaphactory-test metaphactory-with-apps
# to inspect the container created using the previous command use the following command:
# docker inspect metaphactory-test
# to run a command within the container created using the previous command use the following command:
# docker exec -ti metaphactory-test bash
# to delete the container created using the previous command use the following command:
# docker rm -f metaphactory-test
