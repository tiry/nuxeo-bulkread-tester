package org.nuxeo.bulkread.jaxrs;

import java.io.File;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.bulkread.service.BRService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "rooms")
@Produces("text/html;charset=UTF-8")
@Path("/rooms")
public class Api extends ModuleRoot {

    protected static class Result {

        long t0;

        String message;

        Integer nbEntries;

        Result() {
            t0 = System.currentTimeMillis();
        }

        @Override
        public String toString() {

            StringBuffer sb = new StringBuffer();
            long t1 = System.currentTimeMillis();

            if (message != null) {
                sb.append(message);
                sb.append("\n");
            }

            sb.append("Exec time :");
            sb.append((t1 - t0) + " ms");

            if (nbEntries != null) {
                sb.append("\nnb entries :" + nbEntries);
                sb.append("\nThroughput :");
                sb.append(((1000 * (nbEntries + 0.0) / (t1 - t0))) + " object/s");
            }

            return sb.toString();
        }

    }

    @Path("new/{name}")
    @GET
    @Produces("text/plain")
    public String create(@PathParam(value = "name") String name, @QueryParam("branches") Integer branchingFactor,
            @QueryParam("max") Integer maxItems) {
        return doCreate(name, branchingFactor, maxItems);
    }

    @Path("new/{name}")
    @POST
    @Produces("text/plain")
    public String doCreate(@PathParam(value = "name") String name, @QueryParam("branches") Integer nbThreads,
            @QueryParam("max") Integer maxItems) {

        if (nbThreads == null) {
            nbThreads = 5;
        }
        if (maxItems == null) {
            maxItems = 5000;
        }

        Result res = new Result();

        BRService rm = Framework.getService(BRService.class);
        DocumentModel room = rm.createBigFolder(name, maxItems, WebEngine.getActiveContext().getCoreSession(),
                nbThreads);

        res.message = "Folder " + room.getName() + " created";

        return res.toString();
    }

    @GET
    @Path("export/{name}")
    @Produces("text/plain")
    public String export(@PathParam(value = "name") String name) throws Exception {
        return doExport(name);
    }

    @POST
    @Path("export/{name}")
    @Produces("text/plain")
    public String doExport(@PathParam(value = "name") String name) throws Exception {

        Result res = new Result();

        BRService rm = Framework.getService(BRService.class);
        File zip = rm.exportBigFolder(name, WebEngine.getActiveContext().getCoreSession(), "");

        res.message = "Room " + name + " exported as " + zip.getAbsolutePath() + (zip.length() / 1024) + "KB";

        return res.toString();
    }

}
