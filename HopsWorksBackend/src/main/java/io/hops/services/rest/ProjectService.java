/*
 * To change this license header, choose License Headers in AllowedRoles Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.services.rest;

import io.hops.annotations.AllowedRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Ermias
 */
@Path("/project")
@RolesAllowed({"ADMIN", "USER"})
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class ProjectService {
    
    @GET
    @Path("read")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles={AllowedRoles.ALL})
    public Response testREAD(){
        JsonResponse json = new JsonResponse();
        json.setStatus("OK");
        json.setData("Accessing resource that is allowed for all");
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @GET
    @Path("write")
    @Produces(MediaType.APPLICATION_JSON)
    @AllowedRoles(roles={AllowedRoles.OWNER})
    public Response testWRITE(){
        JsonResponse json = new JsonResponse();
        json.setStatus("OK");
        json.setData("Accessing resource that is only allowed for the owner");
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    @GET
    @Path("doSomething")
    @Produces(MediaType.APPLICATION_JSON)
    public Response doSomething(){
        JsonResponse json = new JsonResponse();
        json.setStatus("OK");
        json.setData("Accessing resource that is not annoteted");
        return getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }
    
    private Response.ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setMaxAge(-1);
        cc.setMustRevalidate(true);

        return Response.status(status).cacheControl(cc);
    }
}
