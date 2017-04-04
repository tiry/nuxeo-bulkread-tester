package org.nuxeo.bulkread.service;

import java.io.File;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface BRService {

    DocumentModel createBigFolder(String name, int maxItems, CoreSession session,  int nbThreads);

    File exportBigFolder(String name, CoreSession session, String ppName) throws Exception;

    long getFolderSize( String name, CoreSession session) throws Exception;


}
