package org.nuxeo.bulkread.jaxrs;

import org.nuxeo.ecm.webengine.app.WebEngineModule;

public class ApiModule extends WebEngineModule {

    @Override
    public Class<?>[] getWebTypes() {
        return new Class<?>[] { Api.class };
    }

}
