

## About

### Use case

The target use case is to generate a sub tree export as fast as possible

 - fetch a lot of document in one call
 - export the documents meta-data + blobs

### Constraints

If the goal is to export 1,000 (or more) documents, we have to be careful to avoid any unitary access :

 - lazy loading of DocumentModel schemas or properties
 - loading of the Blobs

 Since there is currently no dedicated API to bulk fetch a schema or properties on a list of documents, we need to find a trick.

## Design

### POC skeleton

This code provides a POC allowing to tests different options:

 - Build a Service exposing the basic API
   + create a sub tree using the RandomImporter
   + export a sub tree (using Core.io but the point is mainly to load all properties and data)
 - Make this run in Unit test
 - Expose via JAX-RS

### Leveraging ES

The first option investigated by this code is to use ES as a loading backend:

 - index Blob inside the `_source` using `base64` 
    - for that we use a custom `JsonESDocumentWriter`
 - use a custom `PageProvider` to load the data
   + for usage of ES loading
   + contribute a custon `EsFetcher`
