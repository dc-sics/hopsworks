package se.kth.hopsworks.rest;

import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
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
import org.apache.commons.codec.binary.Base64;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.bbc.security.ua.PeopleAccountStatus;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.controller.UserStatusValidator;
import se.kth.hopsworks.controller.UsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.BbcGroupFacade;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.Settings;

@Path("/auth")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthService {

    @EJB
    private UserFacade userBean;
    @EJB
    private UsersController userController;
    @EJB
    private UserStatusValidator statusValidator;
    @EJB
    private BbcGroupFacade bbcGroup;
    @EJB
    private NoCacheResponse noCacheResponse;
    @EJB
    Settings settings;
    @EJB
    AuditManager am;

    @GET
    @Path("session")
    @RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response session(@Context SecurityContext sc,
        @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();
        req.getServletContext().log("SESSIONID: " + req.getSession().getId());
        try {
            json.setStatus("SUCCESS");
            json.setData(sc.getUserPrincipal().getName());
        } catch (Exception e) {
            throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                ResponseMessages.AUTHENTICATION_FAILURE);
        }
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }

    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("email") String email,
        @FormParam("password") String password, @FormParam("otp") String otp,
        @Context SecurityContext sc,
        @Context HttpServletRequest req, @Context HttpHeaders httpHeaders)
        throws AppException, MessagingException {

        req.getServletContext().log("email: " + email);
        req.getServletContext().log("SESSIONID@login: " + req.getSession().getId());
        req.getServletContext().log("SecurityContext: " + sc.getUserPrincipal());
        req.getServletContext().log("SecurityContext in user role: " + sc.isUserInRole("HOPS_USER"));
        req.getServletContext().log("SecurityContext in sysadmin role: " + sc.isUserInRole("HOPS_ADMIN"));
        JsonResponse json = new JsonResponse();
        if (email == null || email.isEmpty()) {
            throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Email address field cannot be empty");
        }
        Users user = userBean.findByEmail(email);
        if (user == null) {
            throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "Unrecognized email address. Have you registered yet?");
        }
        String newPassword = null;
        // Add padding if custom realm is disabled
        if (otp == null || otp.isEmpty() && user.getMode() == PeopleAccountStatus.M_ACCOUNT_TYPE.getValue()) {
            otp = AuthenticationConstants.MOBILE_OTP_PADDING;
        }

        if (otp.length() == AuthenticationConstants.MOBILE_OTP_PADDING.length() && user.getMode() == PeopleAccountStatus.M_ACCOUNT_TYPE.getValue()) {
            newPassword = password + otp;
        } else if (otp.length() == AuthenticationConstants.YUBIKEY_OTP_PADDING.length() && user.getMode() == PeopleAccountStatus.Y_ACCOUNT_TYPE.getValue()) {
            newPassword = password + otp + AuthenticationConstants.YUBIKEY_USER_MARKER;
        } else {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Could not recognize the account type. Report a bug.");
        }

        //only login if not already logged in...
        if (sc.getUserPrincipal() == null) {
            if (statusValidator.checkStatus(user.getStatus())) {
                try {

                    req.getServletContext().log("going to login. User status: " + user.
                        getStatus());
                    req.login(email, newPassword);
                    req.getServletContext().log("3 step: " + email);
                    userController.resetFalseLogin(user);
                    am.registerLoginInfo(user, UserAuditActions.LOGIN.name(),
                        UserAuditActions.SUCCESS.name(), req);
                    //if the logedin user has no supported role logout
                    if (!sc.isUserInRole("HOPS_USER") && !sc.isUserInRole("HOPS_ADMIN") && !sc.isUserInRole("AGENT") ) {
                        am.registerLoginInfo(user, UserAuditActions.UNAUTHORIZED.getValue(),
                            UserAuditActions.FAILED.name(), req);
                        userController.setUserIsOnline(user, AuthenticationConstants.IS_OFFLINE);
                        req.logout();

                        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                            "No valid role found for this user");
                    }

                } catch (ServletException e) {
                    userController.registerFalseLogin(user);
                    am.registerLoginInfo(user, UserAuditActions.LOGIN.name(),
                        UserAuditActions.FAILED.name(), req);
                    throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                        ResponseMessages.AUTHENTICATION_FAILURE);
                }
            } else { // if user == null
                throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                    ResponseMessages.AUTHENTICATION_FAILURE);
            }
        } else {
            req.getServletContext().log("Skip logged because already logged in: "
                + email);
        }

        userController.setUserIsOnline(user, AuthenticationConstants.IS_ONLINE);
        //read the user data from db and return to caller
        json.setStatus("SUCCESS");
        json.setSessionID(req.getSession().getId());

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }

    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest req) throws AppException {

        Users user = userBean.findByEmail(req.getRemoteUser());
        JsonResponse json = new JsonResponse();

        try {
            req.logout();
            json.setStatus("SUCCESS");
            req.getSession().invalidate();
            userController.setUserIsOnline(user, AuthenticationConstants.IS_OFFLINE);
            am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
                UserAuditActions.SUCCESS.name(), req);

        } catch (ServletException e) {

            am.registerLoginInfo(user, UserAuditActions.LOGOUT.name(),
                UserAuditActions.FAILED.name(), req);
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Logout failed on backend");
        }
        return Response.ok().entity(json).build();
    }

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(UserDTO newUser, @Context HttpServletRequest req)
        throws AppException, SocketException, NoSuchAlgorithmException {

        byte[] qrCode = null;

        JsonResponse json = new JsonResponse();

        qrCode = userController.registerUser(newUser, req);

        if (settings.findById("twofactor_auth").getValue().equals("true")) {
            json.setQRCode(new String(Base64.encodeBase64(qrCode)));
        } else {
            json.setSuccessMessage(
                "We registered your account request. Please validate you email and we will review your account within 48 hours.");
        }

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }

    @POST
    @Path("registerYubikey")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerYubikey(UserDTO newUser,
        @Context HttpServletRequest req)
        throws AppException, SocketException, NoSuchAlgorithmException {

        JsonResponse json = new JsonResponse();

        userController.registerYubikeyUser(newUser, req);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }

    @POST
    @Path("recoverPassword")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recoverPassword(@FormParam("email") String email,
        @FormParam("securityQuestion") String securityQuestion,
        @FormParam("securityAnswer") String securityAnswer,
        @Context SecurityContext sc,
        @Context HttpServletRequest req) throws AppException {
        JsonResponse json = new JsonResponse();

        userController.recoverPassword(email, securityQuestion, securityAnswer, req);

        json.setStatus("OK");
        json.setSuccessMessage(ResponseMessages.PASSWORD_RESET_SUCCESSFUL);

        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
    }

}
