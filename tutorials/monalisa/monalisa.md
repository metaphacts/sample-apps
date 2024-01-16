# Tutorial: Querying the OpenStreetMap REST service to retrieve geodata


In this tutorial we will show how to configure Ephedra to combine information from a local RDF repository, remote Wikidata SPARQL endpoint, and the OpenStreetMap API in a single query and display it on a map. 

See also the [app project](../../ephedra-mona-lisa/) for a complete solution to this tutorial.


## Prerequisites

* We assume familiarity with Linked Data documents and the SPARQL query language
* We assume that the reader has a running installation of the metaphactory platform


## Introduction

Our simple example use case will deal with museum artifacts and their storage locations. We will retrieve this data from the knowledge graph, combine it using Ephedra with information about geographical borders of these locations from the OpenStreetMap API, and show it as regions on a map.


After going through this post, you will be familiar with the following:

* Configuring a wrapper repository for a remote API service to enable invoking it using SPARQL.
* Configuring a hybrid federation with Ephedra including both RDF repositories and an API service.
* Integrating data from RDF databases and an API service on the fly in a single SPARQL query.
* Visualizing integrated data using the metaphactory UI components.


## Configuration

Our sample dataset contains information about people interested in the "Mona Lisa" painting. From a public RDF repository such as Wikidata we can add information about the "Mona Lisa" painting itself, such as, in which museum it is stored and in which city and district the museum is located. Finally, we can retrieve the exact borders of this region from the OpenStreetMap API and display it as an area on a map.

In order to achieve this we require some configuration for the OpenStreetMap API, particularly using the metaphactory REST servicer wrapper.

### OpenStreetMap Service configuration

A service descriptor for an Ephedra wrapper repository configures the mapping between the input and output parameters of the API and the SPARQL values and variables expressed in the SPARQL query.

In our example, we are dealing with the Nominatim OpenStreetMap search REST API (https://nominatim.openstreetmap.org/). This API expects to receive an HTTP GET query passing the parameters:

```
https://nominatim.openstreetmap.org/search?q=1st%20arrondissement%20of%20Paris&polygon_text=1&format=json&extratags=1
```
The result of this request is a JSON representation:

```
[
  {
    "place_id": 117770801,
    "licence": "Data © OpenStreetMap contributors, ODbL 1.0. http://osm.org/copyright",
    "osm_type": "node",
    "osm_id": 683094599,
    "lat": "48.8644291",
    "lon": "2.3303145",
    "class": "shop",
    "type": "travel_agency",
    "place_rank": 30,
    "importance": 0.00000999999999995449,
    "addresstype": "shop",
    "name": "Paris",
    "display_name": "Paris, Rue de Rivoli, Quartier Vendôme, Paris 1er Arrondissement, Paris, Île-de-France, France métropolitaine, 75001, France",
    "extratags": {
      "contact:city": "Paris",
      "contact:street": "Rue de Rivoli",
      "contact:postcode": "75001",
      "contact:housenumber": "208"
    },
    "boundingbox": [
      "48.8643791",
      "48.8644791",
      "2.3302645",
      "2.3303645"
    ],
    "geotext": "POINT(2.3303145 48.8644291)"
  }
]
```


From this JSON array we need to extract the information we need: the display name of the found city (_display_name_), coordinates of the bounding polygon (_geotext_), and the Wikidata ID of the returned city (_extratags / wikidata_).

(Note that according to its usage policy (https://operations.osmfoundation.org/policies/nominatim/), the Nominatim API cannot be used for production and/or heavy load usage.)

With Ephedra, we can configure a service descriptor on the _Ephedra services_ page.

Navigate to _"/resource/Admin:EphedraServices"_ in your metaphactory installation, and create a new service configuration (say '_openstreetmap-geo_'). Then use the following content.


```
PREFIX sp: <http://spinrdf.org/sp#>
PREFIX spin: <http://spinrdf.org/spin#>
PREFIX spl: <http://spinrdf.org/spl#>
PREFIX osm: <http://www.metaphacts.com/ontologies/osm#>
PREFIX geo: <http://www.opengis.net/ont/geosparql#>
PREFIX ephedra: <http://www.metaphacts.com/ontologies/platform/ephedra#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX sail: <http://www.openrdf.org/config/sail#>
PREFIX : <http://www.metaphacts.com/ontologies/platform/service/custom#>

osm:openstreetmap-geo a ephedra:Service ;
	rdfs:label "A wrapper for the OpenStreetMap service." ; 
	sail:sailType "metaphacts:RESTService" ;
	ephedra:hasSPARQLPattern (
		[
			sp:subject :_result ;
			sp:predicate osm:hasSearchTerm ;
			sp:object :_q
		]
		[
			sp:subject :_result ;
			sp:predicate osm:polygonText ;
			sp:object :_polygon_text
		]
		[
			sp:subject :_result ;
			sp:predicate osm:format ;
			sp:object :_format
		]
		[
			sp:subject :_result ;
			sp:predicate osm:extratags ;
			sp:object :_extratags
		]
		[
			sp:subject :_result ;
			sp:predicate rdfs:label ;
			sp:object :_display_name
		]
		[
			sp:subject :_result ;
			sp:predicate osm:wktGeotext ;
			sp:object :_geotext
		]
		[
			sp:subject :_result ;
			sp:predicate osm:cityName ;
			sp:object :_cityName
		]
	) ;
	spin:constraint
	[
		a spl:Argument ;
		rdfs:comment "search term" ;
		spl:predicate :_q ;
		spl:valueType xsd:string
	] ;
	spin:constraint
	[
		a spl:Argument ;
		rdfs:comment "polygon text flag" ;
		spl:predicate :_polygon_text ;
		spl:defaultValue "1"^^xsd:integer ;
		spl:valueType xsd:integer
	] ;
	spin:constraint
	[
		a spl:Argument ;
		rdfs:comment "format" ;
		spl:predicate :_format ;
		spl:defaultValue "json" ;
		spl:valueType xsd:string
	] ;
	spin:constraint
	[
		a spl:Argument ;
		rdfs:comment "extra tags flag" ;
		spl:predicate :_extratags ;
		spl:defaultValue "1"^^xsd:integer ;
		spl:valueType xsd:integer
	] ;
	spin:column
	[
		a spin:Column ;
		rdfs:comment "result" ;
		spl:predicate :_result ;
		spl:valueType rdfs:Resource;
		ephedra:jsonPath "$"
	] ;
	spin:column
	[
		a spin:Column ;
		rdfs:comment "display name" ;
		spl:predicate :_display_name ;
		spl:valueType xsd:string;
		ephedra:jsonPath "$.display_name"
	] ;
	spin:column
	[
		a spin:Column ;
		rdfs:comment "geotext" ;
		spl:predicate :_geotext ;
		ephedra:jsonPath "$.geotext" ;
		spl:valueType geo:wktLiteral
	] ;
	spin:column
	[
		a spin:Column ;
		rdfs:comment "cityName" ;
		spl:predicate :_cityName ;
		ephedra:jsonPath "$.extratags.contact:city" ;
		spl:valueType xsd:string
	] .
```

This service descriptor tells Ephedra how the inputs and outputs are mapped from the SPARQL query terms to the request parameters and JSON fields of the actual REST API.

Notes: 

* the object of the property _osm:hasSearchTerm_ must carry the value for the search token (i.e. _{ ?result osm:hasSearchTerm "Paris" }_)
* the object of _osm:cityName_ must be an unbound variable (i.e. _{ ?result osm:cityName ?cityName }_ and the value will be extracted from the JSON structure using the JSON path expression _$.extratags.contact:city_. 
* Inputs two the REST requests are modeled as _spin:constraint_, while outputs are modeled using _spin:column_

Note that for the mapping you can use properties from an existing ontology, however, you are also free to come up with special purpose vocabulary (like the _osm:namespace_ in this example). 


### OpenStreetMap Repository Wrapper configuration

After specifying the service descriptor for the REST service, it is required to register the REST service wrapper as a repository in the platform repository manager.

Navigate to _"/resource/Admin:Repositories"_ in your metaphactory installation, and create a new repository configuration (say '_openstreetmap-geo_'). Then use the following content.
 
```
PREFIX sail: <http://www.openrdf.org/config/sail#>
PREFIX repo: <http://www.openrdf.org/config/repository#>
PREFIX repo-sail: <http://www.openrdf.org/config/repository/sail#>
PREFIX ephedra: <http://www.metaphacts.com/ontologies/platform/ephedra#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
PREFIX osm: <http://www.metaphacts.com/ontologies/osm#> 

[] a repo:Repository ;
	repo:repositoryID "openstreetmap-geo" ;
	rdfs:label "Repository to test the generic service wrapper applied to OpenStreetMap." ;
	repo:repositoryImpl [
 		repo:repositoryType "openrdf:SailRepository" ;
 		repo-sail:sailImpl [
			sail:sailType "metaphacts:RESTService" ;
			ephedra:httpMethod "GET" ;
			ephedra:serviceURL "https://nominatim.openstreetmap.org/search" ;
			ephedra:httpHeader [
				ephedra:name "Accept" ;
				ephedra:value "application/json" ;
			] ;
			ephedra:implementsService osm:openstreetmap-geo 
		] 
	] .
```

Notes:

* the property _ephedra:serviceURL_ points to the REST API service URL, e.g. in our case _https://nominatim.openstreetmap.org/search_
* the property _ephedra:implementsService_ should point to the identifier of the service descriptor that we created above (i.e. in our case the prefixed URI _osm:openstreetmap-geo_)
* optionally the HTTP method and additional HTTP headers can be specified using the _ephedra:httpMethod_ amd _ephedra:httpHeader_ properties, respectively
* the newly created repository is backed by the file _config/repositories/openstreetmap-geo.ttl_
* the _repositoryID_ property needs to correspond to the repository identifier

After the repository configuration is defined and saved, the REST wrapper repository is up and running. You can test it by navigating to the SPARQL endpoint page of the metaphactory platform and sending the following query to the _openstreetmap-geo_ repository.

```
PREFIX osm: <http://www.metaphacts.com/ontologies/osm#> 
SELECT ?displayName ?wikidataID WHERE { 
	?result osm:hasSearchTerm "Paris" . 
	?result rdfs:label ?displayName . 
	?result osm:cityName ?cityName . 
}
```


### Ephedra configuration

Although now we can query the REST API with SPARQL, this does not give much added value in comparison with sending HTTP requests directly. The added value comes when the REST API data gets dynamically integrated with our knowledge graph. Ephedra achieves this using SPARQL 1.1 query federation: by using SERVICE blocks in SPARQL queries.

Ephedra federation is configured as a separate repository in the platform that combines other repositories as federation members. Each Ephedra federation has one mandatory default federation member and an arbitrary number of additional ones which are referenced by their alias URIs.

The Ephedra repository can be configured on the _Repository administration_ page, i.e. _"/resource/Admin:Repositories"_ in your metaphactory installation. 

As of the 3.5 release it is easiest to reuse the `defaultEphedra` federation and add the `openstreetmap-geo` repository as additional member:


```
ephedra:serviceMember [
   ephedra:delegateRepositoryID "openstreetmap-geo";
   ephedra:serviceReference <http://www.metaphacts.com/ontologies/platform/repository/federation#openstreetmap-geo>
];
```

For reference, a complete definition of the repository federation may look as follows:

```
@prefix ephedra: <http://www.metaphacts.com/ontologies/platform/ephedra#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix rep: <http://www.openrdf.org/config/repository#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

[] a rep:Repository;
  rep:repositoryID "defaultEphedra";
  rep:repositoryImpl [
      ephedra:defaultMember "proxyToDefault";
      ephedra:serviceMember [
          ephedra:delegateRepositoryID "lookup";
          ephedra:serviceReference <http://www.metaphacts.com/ontologies/repository#lookup>
        ];
      ephedra:serviceMember [
          ephedra:delegateRepositoryID "openstreetmap-geo";
          ephedra:serviceReference <http://www.metaphacts.com/ontologies/platform/repository/federation#openstreetmap-geo>
        ];
      ephedra:writable true;
      rep:repositoryType "metaphacts:EphedraRepository"
    ];
  rdfs:label "Ephedra federation" .
```


The most relevant part is to define a new federation member, i.e. using the _delegateRepositoryID_ and the _serviceReference_ properties. Note that the service reference points to the IRI that can be used in the SERVICE clause.

Note that as of 3.5.0 metaphactory uses `defaultEphedra` as the active default repository. See [here](https://help.metaphacts.com/resource/Help:EphedraAsDefaultRepository) for details.

After creating and applying the Ephedra configuration, you can test it by executing the following example query using the SPARQL interface:

```
PREFIX wd: <http://www.wikidata.org/entity/> 
PREFIX osm: <http://www.metaphacts.com/ontologies/osm#> 
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
PREFIX wdt: <http://www.wikidata.org/prop/direct/> 
SELECT ?label ?wkt WHERE { 
  SERVICE <https://query.wikidata.org/sparql> { 
    wd:Q12418 wdt:P276/wdt:P361* ?museum . 
    ?museum wdt:P131 ?geolocation . 
    ?geolocation rdfs:label ?label . 
    FILTER (LANG(?label) = "en")
  } 
  SERVICE <http://www.metaphacts.com/ontologies/platform/repository/federation#openstreetmap-geo> { 
    ?result osm:hasSearchTerm ?label . 
    ?result osm:wktGeotext ?wkt . 
  }
} LIMIT 1
```

In the metaphactory platform's SPARQL interface the result looks as follows:

![Mona Lisa Test Query](images/monalisa-test-query.png "Mona Lisa Test Query")



## Integrated examples

The following example query fetches the information about the location of the museum containing "Mona Lisa" from Wikidata ("Mona Lisa" is stored in Louvre, which is located in the 1st arrondissement of Paris). This data is retrieved from the remote Wikidata endpoint using the SPARQL 1.1 federation SERVICE keyword.

Furthermore, the borders of the museum location district (that make it possible to draw it on a map) is retrieved from OpenStreetMap. While this clause is also retrieved using a SERVICE keyword, this request is actually sent to a remote OpenStreetMap REST API.

The result of this query is applied in the metaphactory platform to display the location district ("1st arrondissement of Paris") on a map:


```
<semantic-map id='map-result' query='
	PREFIX wd: <http://www.wikidata.org/entity/> 
	PREFIX osm: <http://www.metaphacts.com/ontologies/osm#> 
	PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
	PREFIX wdt: <http://www.wikidata.org/prop/direct/> 
	SELECT ?link ?wkt ?label WHERE { 
		SERVICE <https://query.wikidata.org/sparql> { 
			wd:Q12418 wdt:P276/wdt:P361* ?museum . 
			?museum wdt:P131 ?geolocation . 
			?geolocation rdfs:label ?label . 
			FILTER (LANG(?label) = "en")
		} 
		SERVICE <http://www.metaphacts.com/ontologies/platform/repository/federation#openstreetmap-geo> { 
			?result osm:hasSearchTerm ?label . 
			?result osm:wktGeotext ?wkt . 
		} 
		FILTER(STRSTARTS(STR(?wkt), "POINT")) 
	} LIMIT 1 '
fix-zoom-level=13
tuple-template='<b>{{label.value}}</b>'>  
</semantic-map>
```

In the metaphactory platform's the resulting map looks as follows:

![Mona Lisa Museum on Map](images/monalisa-map.png "Mona Lisa Museum on Map")