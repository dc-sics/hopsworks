package io.hops.hopsworks.apiV2.auth;

import io.hops.hopsworks.api.user.AuthService;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;

@Path("/v2/auth")
@Stateless
@Api(value = "V2 Auth")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthResource {
  
  @Inject
  private AuthService authV1;
  
  @GET
  @Path("session")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response session(@Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    return authV1.session(sc, req);
  }
  
  @POST
  @Path("login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@FormParam("email") String email,
      @FormParam("password") String password, @FormParam("otp") String otp,
      @Context SecurityContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
      throws AppException, MessagingException {
    return authV1.login(email, password, otp, sc, req, httpHeaders);
  }
  
  @GET
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@Context HttpServletRequest req) throws AppException {
    return authV1.logout(req);
  }
  
  @GET
  @Path("isAdmin")
  @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
  public Response login(@Context SecurityContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
      throws AppException, MessagingException {
    return authV1.login(sc, req, httpHeaders);
  }
  
  @POST
  @Path("register")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(UserDTO newUser, @Context HttpServletRequest req)
      throws AppException, SocketException, NoSuchAlgorithmException {
    return authV1.register(newUser, req);
  }
  
  @POST
  @Path("registerYubikey")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerYubikey(UserDTO newUser,
      @Context HttpServletRequest req)
      throws AppException, SocketException, NoSuchAlgorithmException {
    return authV1.registerYubikey(newUser, req);
  }
  
  @POST
  @Path("recoverPassword")
  @Produces(MediaType.APPLICATION_JSON)
  public Response recoverPassword(@FormParam("email") String email,
      @FormParam("securityQuestion") String securityQuestion,
      @FormParam("securityAnswer") String securityAnswer,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    return recoverPassword(email, securityQuestion, securityAnswer, sc, req);
  }
  
}
