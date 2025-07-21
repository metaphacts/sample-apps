# Example app for demonstrating extension

This app provides examples for app extensions.

Currently the following extensions are shown

* REST extension endpoint
* Handlebars extension
* Custom HTML5 component
* Dynamic permission provider for assets based on a property file
* Example for an EventDecorator for `OntologyReadyForReviewEvent`s

The entry point is available on http://localhost:10214/resource/ex:AppExtensions


## Example: Using the Property file based dynamic permission provider

In the following you will see a step-by-step example on how to use the custom property file based dynamic permission provider. It provides access for users to assets, based on role assignments in a property file configuration.

The provider is an extension example for dynamic permissions, see https://help.metaphacts.com/resource/Help:DynamicPermissions#permission-mapping.

### Preparation

* install this app metaphactory
* create local users **sofia**, **tom** and **frank** in metaphactory with the role **knowledge-steward**
* configure the `PropertyFileDynamicPermissionProvider` as `shiroDynamicPermissionProviders` in **config/environment.prop**:

```
shiroDynamicPermissionProviders=PropertyFileDynamicPermissionProvider
```

* configure dynamic permissions for the scenario in the file **config/dynamicPermissions.prop** (placed in the configured security storage, falling back to the regular runtime storage)

```
# In this example dynamic permissions on example-ontology are granted to frank and sofia
#
# id1 and id2 are arbitrary identifiers (must be unique) that group properties together

# Full IRI of the asset (ontology)
id1.iri=https://ontologies.metaphacts.com/example-ontology/1.0
# Username of the user who gets roles on the asset
id1.userName=frank
# The roles (as comma-separated list) the user gets on the asset (owner and/or author)
id1.roles=author
# Type of the asset (ontology/vocabulary)
id1.type=ontology

id2.iri=https://ontologies.metaphacts.com/example-ontology/1.0
id2.userName=sofia
id2.roles=owner
id2.type=ontology
```

### Demo

* create a new ontology as **tom**
    * Name `Example Ontology`
    * IRI `https://ontologies.metaphacts.com/example-ontology/1.0`
* login as **sofia**. Despite not being able to modify other ontologies, she is able to modify the example ontology, as she is assigned the `Owner` role through dynamic permissions
* login as **frank**. He is assigned the `Author` role and can edit the example ontology, but cannot do actions like publishing

For more complex use-cases create different ontologies and corresponding permission configurations.