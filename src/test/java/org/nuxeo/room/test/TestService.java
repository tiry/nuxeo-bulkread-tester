package org.nuxeo.room.test;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.bulkread.service.BRService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy({"org.nuxeo.ecm.core.io", "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.platform.importer.core"})
@LocalDeploy({"org.nuxeo.bulkread.tests", "org.nuxeo.bulkread.tests:elasticsearch-test-contrib.xml"})
public class TestService {

    @Inject
    CoreSession session;

    @Inject
    BRService brs;

    @Inject
    EventService eventService;


    @Test
    public void checkServiceDeployed() throws Exception {
        BRService rs = Framework.getService(BRService.class);
        Assert.assertNotNull(rs);

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        Assert.assertNotNull(esi);

    }

    @Test
    public void shouldCreateFolder() throws Exception {

        String ppName="export_fullES";

        String name="yo";

        DocumentModel folder = brs.createBigFolder(name, 100, session, 1);
        Assert.assertNotNull(folder);

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        Thread.sleep(1000);

        eventService.waitForAsyncCompletion();

        DocumentModelList children = session.getChildren(folder.getRef());

        System.out.println("CHILDREN=###" + children.size());

        File export = brs.exportBigFolder(name, session, ppName);

        System.out.println(export.getAbsolutePath());

    }


}

