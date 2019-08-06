# Deployment of app

In this tutorial we describe how apps can be deployed into a metaphactory container.

Please see https://help.metaphacts.com/resource/Help:Apps for the main documentation on the app mechanism.

## Apps mounted as docker volumes

In order to make an app available in metaphactory, we can make use of the docker volume concept, and basically mount a location of the app on the host machine within the container.

Let's assume the app is locally available at _/Users/home/demo/apps/myApp/_ (e.g. extracted from a zip distribution or retrieved from a version control system like GIT).

Then a volume definition in docker looks like _/Users/home/demo/apps/myApp:/apps/myApp_

This can be either passed to _docker run_ using the _-v_ option or being configured as _volumes_ in the _docker-compose definition file_.

Below is an example for a _docker-compose.override.yml_ file with a custom app being mounted:

```
...
 metaphactory:
    #metaphactory-overwites here
    volumes:
      - /Users/home/demo/apps/myApp:/apps/myApp
    ports:
      - "10214:8080"
...
```
