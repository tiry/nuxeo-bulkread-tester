/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.bulkread.es;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.nuxeo.bulkread.io.JsonESBase64DocumentModelReader;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.elasticsearch.fetcher.EsFetcher;

/**
 * @since TODO
 */
public class ESFetcherWithBlobs extends EsFetcher {

    /**
     * @param session
     * @param response
     * @param repoNames
     */
    public ESFetcherWithBlobs(CoreSession session, SearchResponse response, Map<String, String> repoNames) {
        super(session, response, repoNames);
    }

    @Override
    public DocumentModelListImpl fetchDocuments() {
        DocumentModelListImpl ret = new DocumentModelListImpl(getResponse().getHits().getHits().length);
        DocumentModel doc;
        String sid = getSession().getSessionId();
        for (SearchHit hit : getResponse().getHits()) {
            // TODO: this does not work on multi repo
            doc = new JsonESBase64DocumentModelReader(hit.getSource()).sid(sid).getDocumentModel();
            // Add highlight if it exists
            Map<String, HighlightField> esHighlights = hit.highlightFields();
            if (!esHighlights.isEmpty()) {
                Map<String, List<String>> fields = new HashMap<>();
                for (Map.Entry<String, HighlightField> entry : esHighlights.entrySet()) {
                    String field = entry.getKey();
                    List<String> list = new ArrayList<>();
                    for (Text fragment : entry.getValue().getFragments()) {
                        list.add(fragment.toString());
                    }
                    fields.put(field, list);
                }
                doc.putContextData("highlight", (Serializable) fields);
            }
            ret.add(doc);
        }
        return ret;
    }
}
