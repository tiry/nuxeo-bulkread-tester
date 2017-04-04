/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.bulkread.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch Page provider that converts the NXQL query build by CoreQueryDocumentPageProvider.
 *
 * @since 5.9.3
 */
public class BulkESPageProvider extends ElasticSearchNxqlPageProvider {

    protected static final Log log = LogFactory.getLog(BulkESPageProvider.class);

    private static final long serialVersionUID = 1L;

    public final static String LOAD_BLOB_FROM_ES_PROPERTY = "loadBlobsFromES";

    protected boolean loadBlobsFromES() {
        String value = (String) getProperties().get(LOAD_BLOB_FROM_ES_PROPERTY);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public List<DocumentModel> getCurrentPage() {

        long t0 = System.currentTimeMillis();

        // use a cache
        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }
        error = null;
        errorMessage = null;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Perform query for provider '%s': with pageSize=%d, offset=%d", getName(),
                    getMinMaxPageSize(), getCurrentPageOffset()));
        }
        currentPageDocuments = new ArrayList<>();
        CoreSession coreSession = getCoreSession();
        if (query == null) {
            buildQuery(coreSession);
        }
        if (query == null) {
            throw new NuxeoException(String.format("Cannot perform null query: check provider '%s'", getName()));
        }
        // Build and execute the ES query
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        try {
            NxQueryBuilder nxQuery = new CustomNxQueryBuilder(getCoreSession(), loadBlobsFromES()).nxql(query).offset(
                    (int) getCurrentPageOffset()).limit(getLimit());
            if (searchOnAllRepositories()) {
                nxQuery.searchOnAllRepositories();
            }

            nxQuery.fetchFromElasticsearch();

            EsResult ret = ess.queryAndAggregate(nxQuery);
            DocumentModelList dmList = ret.getDocuments();
            currentAggregates = new HashMap<>(ret.getAggregates().size());
            for (Aggregate<Bucket> agg : ret.getAggregates()) {
                currentAggregates.put(agg.getId(), agg);
            }
            setResultsCount(dmList.totalSize());
            currentPageDocuments = dmList;
        } catch (QueryParseException e) {
            error = e;
            errorMessage = e.getMessage();
            log.warn(e.getMessage(), e);
        }

        // send event for statistics !
        // fireSearchEvent(getCoreSession().getPrincipal(), query, currentPageDocuments, System.currentTimeMillis() -
        // t0);

        return currentPageDocuments;
    }

}
