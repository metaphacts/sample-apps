# Welcome to the Metaphacts samples and tutorials repository

This repository provides different kinds of tutorial as well as full working samples. Tutorials
are grouped by topic and are organized on different levels of expertise.


## App development and Deployment

The SDK provides integrated means of running the platform from Gradle in a Jetty Webserver.
As a pre-requisite the location of the platform's WAR distribution needs to be provided
using the `platformLocation` setting, e.g. in gradle.properties (see also gradle.properties.template).
Before running the platform the first time, make sure to invoke `gradlew prepareEnvironment`.

Additionally, this SDK provides convenience for developing, running and deploying apps. Artifacts
can be produced using the `gradlew appZip` command and can be found in `target/app`. For development
apps can be seamlessly deployed in the running application by specifying the `apps` setting, 
e.g. in `gradle.properties`.

### Features

* build app artifacts using `gradlew appZip`
* develop against a binary WAR distribution of the platform
* IDE support for Eclipse (e.g. project and classpath generation)
* run the platform locally within a provided Jetty web-server (without requiring docker): `gradlew appRun`
* automatically deploy apps in the local Jetty web-server

### Pre-Requisites

* JDK 1.8 installed on the local machine, JAVA-HOME pointing to the installation
* Copy of a binary WAR distribution of a platform release on the local host
* (Optionally) Eclipse as IDE

### Obtaining a WAR distribution of the platform

The WAR distribution of the platform can be obtained from a running docker-compose environment:

```
docker cp <container-id>:/var/lib/jetty/webapps/ROOT.war platform.war
```

In the above command replace `<container-id>` with the ID of the running container. Hint: use `docker ps` to list the running containers.


### Getting Started

* Setup the environment
    * Copy `gradle.properties.template` as `gradle.properties` and adjust the location of the WAR distribution
    * Optionally adjust the apps setting to point to the desired target app
* Prepare the environment
    * Run `gradlew prepareEnvironment`
* Run the integrated application with the sample app `my-first-app`
    * Run `gradlew appRun`
* Open http://localhost:10214 and login with `admin/admin`

### Useful commands and settings

```
./gradlew clean appZip
./gradlew prepareEnvironment
./gradlew appRun
./gradlew -Ddebug=true appRun
./gradlew cleanEclipse eclipse
```

Available settings:

System properties can be defined using `-Dprop=value` in the `./gradlew` command (e.g. `./gradlew -Ddebug=true appRun`) or 
in `gradle.properties` in the project root folder using the prefix `systemProp.` (see also `gradle.properties.template`).

```
runtimeDirectory => location of the runtime directory (relative to root project). Default: runtime
log => log4j configuration profile (supported values: log4j2 log4j2-debug, log4j2-trace, log4j2-trace2) Default: log4j2-debug
debug => whether JVM debug is enabled (default port 5005)
debug.port => JVM debug port. Default: 5005
debug.suspend => suspend on debug (true|false). Default: false
```

### Automatic deployment of apps

In addition to starting the main platform, the SDK also supports to automatically deploy app artifacts.

The app to be deployed can be specified using the `apps` setting in `gradle.properties`, see `gradle.properties.template` for an example. The setting can be set to the _simple project name_, which basically corresponds to the folder that defines the app. Note that apps must be located inside the SDK environment to be detected by the Gradle build system, see _Remarks_ for further details.

### Remarks

* New apps can be added by creating a folder for the app (e.g. `my-new-app`) and providing the app content. It is required to have a `plugin.properties` file for the detection of apps. See https://help.metaphacts.com/resource/Help:Apps for further details
* The runtime directory by default is `runtime`. Any changes made in the running system (e.g. templates) are written here. If content should become part of the app, the respective items need to be manually copied into the app's folder

## Ephedra tutorials

The following list contains a few compiled Ephedra tutorials.

* Using the REST service wrapper to query geospatial data from OpenStreetMap and combine it with structural information from the Wikidata knowledge graph. Example using Mona Lisa data. [[tutorial](tutorials/monalisa/monalisa.md)], [[app](ephedra-mona-lisa/)]
* Custom service wrapper for a Weather API [[tutorial](tutorials/weather/weather.md)], [[app](ephedra-custom-weatherApi/)]
* Custom service wrapper for querying Linked Data Documents [[tutorial](tutorials/dblp/dblp.md)], [[app](ephedra-custom-linkedDataDocuments/)]