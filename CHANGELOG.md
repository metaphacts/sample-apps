# Changelog

All notable changes to the metaphactory sample apps are documented here.


## 6.1.0

- Update sample apps for metaphactory 6.1.0


## 6.0.0

- Update sample apps for metaphactory 6.0.

### Changed
- Sample apps now target the metaphactory 6.0 SDK (Jakarta EE 11, Jetty 12 EE11, RESTEasy).
- `prepareEnvironment.sh` now points at the 6.0 release line (was `metaphacts/metaphactory:5.9.0`). 
- Local builds now require **JDK 25** (the SDK sets `sourceCompatibility = 25`); previous releases required JDK 21.

### Migration notes
- Source-level Jakarta migration was already completed in metaphactory 5.0 (`javax.* → jakarta.*`, `@Singleton → @ApplicationScoped`); no further code changes are required for apps that already track the 5.x sample-apps branch.
- The JAX-RS implementation in the platform changed from Jersey to RESTEasy. Apps that only consume the standard `jakarta.ws.rs.*` API are unaffected. Apps that previously depended on Jersey-internal classes (e.g. `org.glassfish.jersey.*`) must be ported to standard JAX-RS or to RESTEasy equivalents.

## 5.9.0
- Update sample apps for metaphactory 5.9.
- Example event decorator for `OntologyReadyForReviewEvent`.
- `app-with-extensions` updated to use `web-extensions.json`.

## 5.8.0
- Update sample apps for metaphactory 5.8.
- Align asset type code usage with metaphactory 5.8.

## 5.7.0
- Switch to SDK bundled in metaphactory; automated setup via `prepareEnvironment.sh`.

## 5.6.0
- Update SDK and sample app dependencies for 5.6.
- Add support for running Gretty with Jetty 11 after migrating product to Jetty 12.

## 5.0.0
- Migrate to Jakarta EE: `javax.* → jakarta.*` across all sample apps, JUnit 4 → JUnit 5, Gradle 8.1.1, RDF4J deprecation cleanup.
