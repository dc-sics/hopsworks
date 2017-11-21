/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.exception.EncryptionMasterPasswordException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/admin")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SystemAdminService {
  
  private final Logger LOG = Logger.getLogger(SystemAdminService.class.getName());
  
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private NoCacheResponse noCacheResponse;
  
  /**
   * Admin endpoint that changes the master encryption password used to encrypt the certificates' password
   * stored in the database.
   * @param sc
   * @param request
   * @param oldPassword Current password
   * @param newPassword New password
   * @return
   * @throws AppException
   */
  @POST
  @Path("/changeMasterEncryptionPass")
  public Response changeMasterEncryptionPassword(@Context SecurityContext sc, @Context HttpServletRequest request,
      @FormParam("oldPassword") String oldPassword, @FormParam("newPassword") String newPassword)
    throws AppException {
    LOG.log(Level.FINE, "Requested master encryption password change");
    try {
      String userEmail = sc.getUserPrincipal().getName();
      certificatesMgmService.checkPassword(oldPassword, userEmail);
      certificatesMgmService.resetMasterEncryptionPassword(newPassword, userEmail);
  
      JsonResponse response = new JsonResponse();
      response.setStatus("201");
      response.setSuccessMessage(ResponseMessages.MASTER_ENCRYPTION_PASSWORD_CHANGE);
  
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
    } catch (EncryptionMasterPasswordException ex) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(), ex.getMessage());
    } catch (IOException ex) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error while reading master " +
          "password file: " + ex.getMessage());
    }
  }
}
