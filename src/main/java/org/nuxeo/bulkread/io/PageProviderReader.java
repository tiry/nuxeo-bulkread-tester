/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentTreeReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.bulkread.io;

import java.io.IOException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelReader;
import org.nuxeo.ecm.platform.query.api.PageProvider;

public class PageProviderReader extends DocumentModelReader {

    protected PageProvider<DocumentModel> pp;

    protected int idx = 0;

    public PageProviderReader(CoreSession session, PageProvider<DocumentModel> pp) {
        super(session);
        this.pp = pp;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public ExportedDocument read() throws IOException {
        if (pp.isNextEntryAvailable()) {
            pp.nextEntry();
            return new ExportedDocumentImpl(pp.getCurrentEntry(), inlineBlobs);
        } else {
            return null;
        }
    }

}
