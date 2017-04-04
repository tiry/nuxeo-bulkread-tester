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

import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.elasticsearch.fetcher.EsFetcher;
import org.nuxeo.elasticsearch.fetcher.Fetcher;
import org.nuxeo.elasticsearch.fetcher.VcsFetcher;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;

public class CustomNxQueryBuilder extends NxQueryBuilder {

    protected boolean readBlobFromES;

    public CustomNxQueryBuilder(CoreSession coreSession, boolean readBlobFromES) {
        super(coreSession);
        this.readBlobFromES = readBlobFromES;
    }

    @Override
    public boolean isFetchFromElasticsearch() {
        return true;
    }

    @Override
    public Fetcher getFetcher(SearchResponse response, Map<String, String> repoNames) {
        if (isFetchFromElasticsearch()) {
            if (readBlobFromES) {
                return new ESFetcherWithBlobs(getSession(), response, repoNames);
            } else {
                return new EsFetcher(getSession(), response, repoNames);
            }
        }
        return new VcsFetcher(getSession(), response, repoNames);
    }

}
