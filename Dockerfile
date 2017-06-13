FROM nuxeo:8.10
USER root
RUN curl -fsSL "https://github.com/tiry/nuxeo-bulkread-tester/releases/download/0.1/nuxeo-bulkread-tester-1.0-SNAPSHOT.jar" -o /opt/nuxeo/server/nxserver/bundles/nuxeo-bulkread-tester-1.0-SNAPSHOT.jar \
 && chown 1000:0 /opt/nuxeo/server/nxserver/bundles/nuxeo-bulkread-tester-1.0-SNAPSHOT.jar \
 && curl -fsSL https://maven-eu.nuxeo.org/nexus/content/repositories/public-releases/org/nuxeo/ecm/platform/nuxeo-importer-core/8.10/nuxeo-importer-core-8.10.jar -o /opt/nuxeo/server/nxserver/bundles/nuxeo-importer-core-8.10.jar \
 && chown 1000:0 /opt/nuxeo/server/nxserver/bundles/nuxeo-importer-core-8.10.jar
USER 1000

