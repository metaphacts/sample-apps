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
* Docker-based build, e.g. for building in a CI/CD pipeline

### Pre-Requisites

When performing local development, the following pre-requisites are required:

* JDK 17 installed on the local machine, JAVA-HOME pointing to the installation
* Copy of a binary WAR distribution of a platform release on the local host
* (Optionally) Eclipse as IDE

Note: as of metaphactory 5.0 we have switch to Jakarta EE. The SDK and sample apps are adjusted accordingly. Earlier releases of metaphactory are not compatible. Apps written against an earlier version of the SDK may require migration.

In order to build apps without local installation of all dependencies, the Docker-based build can be used. In this case, the requirements listed above are optional. See below for details on the Docker-based build.

### Obtaining a WAR distribution of the platform

When not using the Docker-based build, the WAR distribution of the platform can be obtained from a running docker-compose environment:

```
docker cp <container-id>:/var/lib/jetty/webapps/ROOT.war platform.war
```

In the above command replace `<container-id>` with the ID of the running container. Hint: use `docker ps` to list the running containers.

__Note__: when updating the platform version it is required to re-run `gradlew prepareEnvironment`


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
./gradlew cleanRuntimeDir
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

## Docker-based Build

In order to build apps without local installation of all dependencies, the Docker-based build can be used.

The provided `Dockerfile` fetches all dependencies and performs the build steps as part of building a Docker image. This can be performed as part of a CI/CD pipeline or locally on a developer machine.

In the provided example, the outcome is a Docker image which contains the apps "baked into" the container as described in the [App Deployment documentation](https://help.metaphacts.com/resource/Help:AppDeployment#container-based-deployment-baked-in). 

The apps are placed into the `/installedapps` folder within the container and are not loaded automatically. They can be activated when starting the container, either by setting the `MP_APP_<N>` environment variables within the `Dockerfile` (see comment at the end of the file) or when launching the container, e.g. as part of a docker-compose file or Kubernetes Pod definition.

### Build steps

The provided `Dockerfile` is expected to be in the root folder of the project. The steps described below will all apps contained in the project folder, with each app defined in its own sub folder. See the folder structure of the [sample apps](https://github.com/metaphacts/sample-apps/) for reference. The app SDK files and folders (`.sdk`, `gradle`, `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`) need to be present as well.

1. customize the `Dockerfile` if required (see comments within the file)
2. build the container with apps use the following command (note the `.` at the end of the command!):

```
docker build --build-arg METAPHACTORY_VERSION=5.5.0 -t metaphactory-with-apps:5.5.0 .
```

See the reference of the [docker build command](https://docs.docker.com/reference/cli/docker/buildx/build/) for all available arguments.

Note: this will perform the build within the container and hence **copy** all files within the current folder into the temporary container for the build.
Building will fetch dependencies like Java libraries, etc. which requires access to the Internet. The downloaded dependencies are not cached, so this will be re-downloaded on each run.

3. Ensure that the target container was created using the following command:

```
docker image ls metaphactory-with-apps
```

This will show something like

```
REPOSITORY             TAG       IMAGE ID       CREATED        SIZE
metaphactory-with-apps   latest    6ef60382ad62   19 hours ago   1.19GB
```

4. To run the container with the app `myapp` for local testing use the following command:

```
docker run --detach --publish 10214:8080 --env MP_APP_1=/installedapps/myapp --name metaphactory-test metaphactory-with-apps:5.5.0
```

Note: multiple apps can be activated by adding multiple environment variables. Alternatively, this can be done directly in the `Dockerfile`, see the comments at the bottom of the file.

5. delete the temporary docker container

```
docker rm -f metaphactory-test
```



### Extract apps from the Docker build

The build steps described above will create a metaphactory image with the apps "baked in". In order to get just the apps, they can be extracted using the following commands:

1. create (but don't run) a temporary container based on the image created above:

```
docker create --name image-with-apps metaphactory-with-apps:5.5.0
```

2. copy the app files from the container to the local file system into the current folder:

Zipped apps (used for easy distribution or installation in existing metaphactory instances):

```
docker cp image-with-apps:/zippedapps .
```

Extracted apps (used for local execution):

```
docker cp image-with-apps:/installedapps .
```

3. delete the temporary docker container

```
docker rm image-with-apps
```

## Ephedra tutorials

The following list contains a few compiled Ephedra tutorials.

* Using the REST service wrapper to query geospatial data from OpenStreetMap and combine it with structural information from the Wikidata knowledge graph. Example using Mona Lisa data. [[tutorial](tutorials/monalisa/monalisa.md)], [[app](ephedra-mona-lisa/)]
* Using the SQL service wrapper to access movie data from a Postgres service. [[app](ephedra-sql-dvdrental/)]
* Custom service wrapper for querying Linked Data Documents [[tutorial](tutorials/dblp/dblp.md)], [[app](ephedra-custom-linkedDataDocuments/)]
