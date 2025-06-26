# Welcome to the Metaphacts samples and tutorials repository

This repository provides different kinds of tutorial as well as full working samples. Tutorials
are grouped by topic and are organized on different levels of expertise.

## App development and Deployment

In order to develop apps with access to Java development extensions the metaphactory SDK is required to be set up.
See the chapters below on how this can be done automatically or manually.

### Setting up the SDK automatically

As of metaphactory 5.7 the SDK is bundled within the metaphactory docker image.
The `prepareEnvironment.sh` shell script can be used to setup the environment using a given metaphactory distribution.
The script by default uses the current metaphactory release.

Note: As a prerequisite the script needs to have `docker` installed.

```
Usage of prepareEnvironment.sh:

./prepareEnvironment.sh [OPTIONS]
OPTIONS:
  -f  force update of SDK, even if the metaphactory version did not change
  -h  display help
  -i <image>  use custom docker image
Examples:
  - with custom docker image: "./prepareEnvironment.sh -i metaphacts/metaphactory:5.7.0"
```

### Setting up the SDK manually

Copy the `sdk.zip` from the metaphactory distribution into this folder and extract it.
As a prerequisite the location of the platform's WAR distribution needs to be provided
using the `platformLocation` setting, e.g. in gradle.properties (see also gradle.properties.template).
Before running the platform the first time, make sure to invoke `./gradlew prepareEnvironment`.

### SDK

The SDK provides integrated means of running the platform from Gradle in a Jetty Webserver.
Additionally, this SDK provides convenience for developing, running and deploying apps.
For further documentation on the usage of the SDK refer to `README-SDK.md` (available once the SDK has been set up).

### Running the example app

- Setup which app(s) to run
  - Adjust the `apps` setting in `gradle.properties` to point to the desired target app (e.g., `my-first-app`)
- Run the integrated application with the sample app `my-first-app`
  - Run `./gradlew appRun`
- Open http://localhost:10214 and login with `admin/admin`

## Ephedra tutorials

The following list contains a few compiled Ephedra tutorials.

- Using the REST service wrapper to query geospatial data from OpenStreetMap and combine it with structural information from the Wikidata knowledge graph. Example using Mona Lisa data. [[tutorial](tutorials/monalisa/monalisa.md)], [[app](ephedra-mona-lisa/)]
- Using the SQL service wrapper to access movie data from a Postgres service. [[app](ephedra-sql-dvdrental/)]
- Custom service wrapper for querying Linked Data Documents [[tutorial](tutorials/dblp/dblp.md)], [[app](ephedra-custom-linkedDataDocuments/)]
