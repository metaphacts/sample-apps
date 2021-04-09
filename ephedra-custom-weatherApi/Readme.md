# Example app for integration of a Weather API service

**Deprecated**: this example is deprecated as DarkSky has discontinued their public Weather API service. The app is kept for documentation purposes only.


This app demonstrates how a custom weather API service can be integrated and accessed via Ephedra. As a use-case we query the geo-coordinates from the Wikidata knowledge graph, and display the three day forecast for the corresponding location.

A full tutorial describing the setup and scenario in detail is available [here](../tutorials/weather/weather.md).

Before deployment make sure to enter your valid API key to `config/repositories/weather-api.ttl`.

The basic example is available on http://localhost:10214/resource/?uri=http://www.example.org/WeatherDemo


