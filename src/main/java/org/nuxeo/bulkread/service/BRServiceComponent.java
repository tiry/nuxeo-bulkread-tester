package org.nuxeo.bulkread.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.bulkread.io.PageProviderReader;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration;
import org.nuxeo.ecm.platform.importer.filter.EventServiceConfiguratorFilter;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class BRServiceComponent extends DefaultComponent implements BRService {

    protected static final Log log = LogFactory.getLog(BRServiceComponent.class);

    protected static ImporterLogger inporterLogger;

    public ImporterLogger getLogger() {
        if (inporterLogger == null) {
            inporterLogger = new BufferredLogger(getJavaLogger());
        }
        return inporterLogger;
    }

    protected Log getJavaLogger() {
        return log;
    }

    @Override
    public DocumentModel createBigFolder(String name, int maxItems, CoreSession session, int nbThreads) {


        int branchingFactor = nbThreads;
        int batchSize = 200;

        getLogger().info("Init Random text generator");
        SourceNode source = RandomTextSourceNode.init(maxItems, 8, true);
        getLogger().info("Random text generator initialized");

        DocumentModel room = session.createDocumentModel("/", name, "Workspace");
        room.setPropertyValue("dc:title", name);
        room = session.createDocument(room);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction(3000);

        ImporterRunnerConfiguration configuration = new ImporterRunnerConfiguration.Builder(source,
                room.getPathAsString(), getLogger()).skipRootContainerCreation(true).batchSize(batchSize).nbThreads(
                branchingFactor).build();
        GenericMultiThreadedImporter runner = new GenericMultiThreadedImporter(configuration);
        ImporterFilter filter = new EventServiceConfiguratorFilter(true, true, false, true, false) {

            @Override
            public void handleBeforeImport() {
                super.handleBeforeImport();
                EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
                eventAdmin.setListenerEnabledFlag("elasticSearchInlineListener", false);
            }

            @Override
            public void handleAfterImport(Exception e) {
                super.handleAfterImport(e);
                EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
                //eventAdmin.setListenerEnabledFlag("elasticSearchInlineListener", true);
            }
        };
        runner.addFilter(filter);

        Thread importer = new Thread(runner);
        importer.run();

        room.setPropertyValue("dc:description", "" + GenericMultiThreadedImporter.getCreatedDocsCounter());
        return session.saveDocument(room);
    }

    @Override
    public long getFolderSize( String name, CoreSession session) throws Exception {

        DocumentModel folder = session.getDocument(new org.nuxeo.ecm.core.api.PathRef("/" + name));

        String sizeStr = (String) folder.getPropertyValue("dc:description");
        if (StringUtils.isEmpty(sizeStr)) {
            return -1;
        } else {
            return Long.parseLong(sizeStr);
        }
    }

    @Override
    public File exportBigFolder(String folderName, CoreSession session, String ppName) throws Exception {

        File archive = File.createTempFile("room-io-archive", "zip");

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);

        PageProviderDefinition ppdef = pps.getPageProviderDefinition(ppName);

        HashMap<String, Serializable> props = new HashMap<>();
        props.put(ElasticSearchNxqlPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        long pageSize = 50;

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pps.getPageProvider(ppName, ppdef, null, null, pageSize, (long) 0, props, folderName);

        DocumentReader reader = new PageProviderReader(session, pp);
        DocumentWriter writer = new NuxeoArchiveWriter(archive);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();


        writer.close();

        return archive;
    }




    protected long doHeavyReads(CoreSession session, int nbCycles) {

        Long nbReads = 0L;

        Random rnd = new Random(System.currentTimeMillis());

        List<String> uuids = new ArrayList<String>();

        for (int i = 0; i < nbCycles; i++) {
            String query = "SELECT * FROM Document WHERE ecm:name like '";
            query += "file-" + rnd.nextInt(10) + "-" + +rnd.nextInt(10) + "%";
            query += "' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";
            query += " order by dc:modified asc ";
            DocumentModelList docs = session.query(query, 200);
            if (docs.size() > 0) {
                uuids.add(docs.get(0).getParentRef().toString());
                nbReads+=docs.size();
            }
        }

        for (String uuid : uuids) {
            DocumentModelList docs = session.query("select * from Document where ecm:ancestorId ='" + uuid
                    + "' order by dc:modified asc ", 1000);
            nbReads+=docs.size();
        }
        return nbReads;
    }


    public Double heavyReads(CoreSession session, int nbThreads, int nbCycles) throws Exception {

        long t0 = System.currentTimeMillis();
        AtomicLong counter = new AtomicLong();

        final String repoName = session.getRepositoryName();

        List<Thread> workers = new ArrayList<Thread>();

        for (int i = 0; i < nbThreads; i++) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    new UnrestrictedSessionRunner(repoName) {
                        @Override
                        public void run() {
                            TransactionHelper.startTransaction();
                            counter.addAndGet(doHeavyReads(session, nbCycles));
                            TransactionHelper.commitOrRollbackTransaction();
                        }
                    }.runUnrestricted();
                }
            });

            t.start();
            workers.add(t);
        }

        boolean completed = false;

        while (!completed) {
            completed = true;
            for (Thread worker : workers) {
                if (worker.isAlive()) {
                    completed = false;
                    break;
                }
            }
            Thread.sleep(100);
        }

        return counter.get() / ((System.currentTimeMillis() -t0)/1000.0);
    }
}
