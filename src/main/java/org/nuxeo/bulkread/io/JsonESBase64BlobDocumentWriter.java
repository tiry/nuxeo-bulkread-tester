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
package org.nuxeo.bulkread.io;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.automation.core.util.DateTimeFormat;
import org.nuxeo.ecm.automation.core.util.JSONPropertyWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

@Provider
@Produces({ JsonESDocumentWriter.MIME_TYPE })
public class JsonESBase64BlobDocumentWriter extends JsonESDocumentWriter {

    public static final long MAX_BLOB_SIZE = 10 * 1024;

    /**
     * @since 7.2
     */
    @Override
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc, String[] schemas) throws IOException {
        if (schemas == null || (schemas.length == 1 && "*".equals(schemas[0]))) {
            schemas = doc.getSchemas();
        }
        for (String schema : schemas) {
            _writeProperties(jg, doc, schema, null);
        }
    }

    protected static void _writeProperties(JsonGenerator jg, DocumentModel doc, String schema, ServletRequest request)
            throws IOException {
        Collection<Property> properties = doc.getPropertyObjects(schema);
        if (properties.isEmpty()) {
            return;
        }

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        String prefix = schemaManager.getSchema(schema).getNamespace().prefix;
        if (prefix == null || prefix.length() == 0) {
            prefix = schema;
        }
        prefix = prefix + ":";

        String blobUrlPrefix = null;
        if (request != null) {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            blobUrlPrefix = VirtualHostHelper.getBaseURL(request) + downloadService.getDownloadUrl(doc, null, null)
                    + "/";
        }

        for (Property p : properties) {
            if (p.getType().getName().equals("content")) {

                if (p.getValue() != null) {
                    Blob blob = (Blob) p.getValue();

                    jg.writeFieldName(p.getName());
                    jg.writeStartObject();

                    if (blob.getFilename() != null) {
                        jg.writeStringField("name", blob.getFilename());
                    }
                    if (blob.getMimeType() != null) {
                        jg.writeStringField("mime-type", blob.getMimeType());
                    }
                    if (blob.getEncoding() != null) {
                        jg.writeStringField("encoding", blob.getEncoding());
                    }
                    jg.writeNumberField("length", blob.getLength());

                    if (blob.getLength() < MAX_BLOB_SIZE) {
                        String b64 = Base64.encodeBase64String(blob.getByteArray());
                        jg.writeStringField("base64", b64);
                    } else {
                        jg.writeStringField("digest", blob.getDigest());

                    }
                    jg.writeEndObject();

                }

            } else {
                jg.writeFieldName(prefix + p.getField().getName().getLocalName());
                JSONPropertyWriter.writePropertyValue(jg, p, DateTimeFormat.W3C, blobUrlPrefix);
            }
        }
    }

}
