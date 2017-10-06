package io.hops.hopsworks.apiV2.currentUser;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.dataset.DatasetRequest;
import io.hops.hopsworks.common.dao.dataset.DatasetRequestFacade;
import io.hops.hopsworks.common.dao.message.Message;
import io.hops.hopsworks.common.dao.message.MessageFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.message.MessageController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.elasticsearch.common.Strings;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.logging.Logger;

import static io.hops.hopsworks.apiV2.Util.except;

@Api("V2 Messages")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MessagesResource {

  private final static Logger logger = Logger.getLogger(MessagesResource.class.
          getName());
  @EJB
  private MessageController msgController;
  @EJB
  private MessageFacade msgFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DatasetRequestFacade dsReqFacade;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Path("/inbox")
  @ApiOperation(value = "Get all messages in the user's inbox", response = Message.class, responseContainer = "List",
      notes =  "Get all messages in the user's inbox")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getInbox(@Context SecurityContext sc) throws AppException {
    String email = sc.getUserPrincipal().getName();
    List<Message> list = msgFacade.getInbox(getByEmail(email));
    GenericEntity<List<Message>> msgs
            = new GenericEntity<List<Message>>(list) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            msgs).type(MediaType.APPLICATION_JSON).build();
  }
  
  
  
  @GET
  @Path("/inbox")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getInboxFiltered(@Context SecurityContext sc,
      @QueryParam("filter") String filter) throws AppException {
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    if (user == null){
      except(Response.Status.BAD_REQUEST, "User doesn't exist.");
    }
    
    List<Message> result = null;
    if ("unread".equals(filter)){
      result = msgFacade.getInboxByUnread(user, true);
    } else if ("read".equals(filter)) {
      result = msgFacade.getInboxByUnread(user, false);
    } else {
      except(Response.Status.BAD_REQUEST, "Incorrect filter type.");
    }
    
    return Response.ok(new GenericEntity<List<Message>>(result){})
        .type(MediaType.APPLICATION_JSON).build();
  }
  
  @GET
  @Path("/inbox/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getInboxMessage(@PathParam("id") Integer id, @Context
      SecurityContext sc) throws AppException {
    
    List<Message> inbox = getUserInbox(sc.getUserPrincipal().getName());
    for (Message message : inbox){
      if (message.getId().equals(id)){
        return Response.ok(message).type(MediaType.APPLICATION_JSON).build();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status
        .NOT_FOUND).build();
  }
  
  private List<Message> getUserInbox(String email) throws AppException {
    Users user = userFacade.findByEmail(email);
    if (user == null){
      except(Response.Status.BAD_REQUEST, "User with email "+ email + " not found.");
    }
    return msgFacade.getAllMessagesTo(user);
  }

  @GET
  @Path("/trash")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllDeletedMessagesByUser(@Context SecurityContext sc) {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    List<Message> list = msgFacade.getAllDeletedMessagesTo(user);
    GenericEntity<List<Message>> msgs
            = new GenericEntity<List<Message>>(list) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(msgs).type(MediaType.APPLICATION_JSON).build();
  }
  
  @GET
  @Path("/trash/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getTrashMessage(@PathParam("id") Integer id, @Context
      SecurityContext sc){
    List<Message> trash = getUserTrash(sc.getUserPrincipal().getName());
    for (Message message : trash){
      if (message.getId().equals(id)){
        return Response.ok(message).type(MediaType.APPLICATION_JSON).build();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status
        .NOT_FOUND).build();
  }
  
  private List<Message> getUserTrash(String email){
    Users user = userFacade.findByEmail(email);
    return msgFacade.getAllDeletedMessagesTo(user);
  }

  @PUT
  @Path("inbox/{msgId}/read")
  @Produces(MediaType.APPLICATION_JSON)
  public Response markAsReadInInbox(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      except(Response.Status.NOT_FOUND, "Message not found.");
    }
    //Delete Dataset request from the database
    if (!Strings.isNullOrEmpty(msg.getSubject())) {
      DatasetRequest dsReq = dsReqFacade.findByMessageId(msg);
      if (dsReq != null) {
        dsReqFacade.remove(dsReq);
      }
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setUnread(false);
    msgFacade.update(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
  @PUT
  @Path("trash/{msgId}/read")
  @Produces(MediaType.APPLICATION_JSON)
  public Response markAsReadInTrash(@PathParam("msgId") Integer msgId,
      @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      except(Response.Status.BAD_REQUEST,
          "Message not found.");
    }
    //Delete Dataset request from the database
    if (!Strings.isNullOrEmpty(msg.getSubject())) {
      DatasetRequest dsReq = dsReqFacade.findByMessageId(msg);
      if (dsReq != null) {
        dsReqFacade.remove(dsReq);
      }
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setUnread(false);
    msgFacade.update(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("/trash/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response moveToTrash(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      except(Response.Status.BAD_REQUEST,
              "Message not found.");
    }
    //Delete Dataset request from the database
    if (!Strings.isNullOrEmpty(msg.getSubject())) {
      DatasetRequest dsReq = dsReqFacade.findByMessageId(msg);
      if (dsReq != null) {
        dsReqFacade.remove(dsReq);
      }
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setDeleted(true);
    msgFacade.update(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @PUT
  @Path("/inbox/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response restoreFromTrash(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(msgId);
    if (msg == null) {
      except(Response.Status.BAD_REQUEST,
              "Message not found.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    msg.setDeleted(false);
    msgFacade.update(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("/inbox/{msgId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteMessageInInbox(@PathParam("msgId") Integer msgId,
          @Context SecurityContext sc) throws AppException {
    String email = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(email);
    deleteMessage(user, msgId);
    return Response.ok().build();
  }
  
  @DELETE
  @Path("/trash/{msgId}")
  public Response deleteMessageInTrash(@PathParam("msgId") Integer msgId,
      @Context SecurityContext sc) throws AppException {
    String email = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(email);
    deleteMessage(user, msgId);
    return Response.ok().build();
  }
  
  private void deleteMessage(Users user, Integer msgId) throws AppException {
    Message msg = msgFacade.find(msgId);
    if (msg == null){
      except(Response.Status.NOT_FOUND, "Message not found");
    }
    checkMsgUser(msg, user);
    msgFacade.remove(msg);
  }

  @DELETE
  @Path("/trash")
  @Produces(MediaType.APPLICATION_JSON)
  public Response emptyTrash(@Context SecurityContext sc) throws AppException {
    JsonResponse json = new JsonResponse();
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    int rowsAffected = msgFacade.emptyTrash(user);
    json.setSuccessMessage(rowsAffected + " messages deleted.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/sent")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  public Response reply(@QueryParam("replyTo") Integer replyToId,
          String content,
          @Context SecurityContext sc) throws AppException {
    String eamil = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(eamil);
    Message msg = msgFacade.find(replyToId);
    if (msg == null) {
      except(Response.Status.BAD_REQUEST,
              "Message not found.");
    }
    if (content == null) {
      except(Response.Status.BAD_REQUEST,
              "No content.");
    }
    checkMsgUser(msg, user);//check if the user is the owner of the message
    try {
      msgController.reply(user, msg, content);
    } catch (IllegalArgumentException e) {
      except(Response.Status.BAD_REQUEST,
              e.getMessage());
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            msg).build();
  }
  
  @GET
  @Path("/sent")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSent(@Context SecurityContext sc) throws AppException {
    except(Response.Status.NOT_IMPLEMENTED,
        "Not implemented yet.");
    return null;
  }
  
  private Users getByEmail(String email) throws AppException {
    Users byEmail = userFacade.findByEmail(email);
    if (byEmail == null){
      except(Response.Status.BAD_REQUEST, "User with email: " +
          email + " not found.");
    }
    return byEmail;
  }
  
  private void checkMsgUser(Message msg, Users user) throws AppException {
    if (!msg.getTo().equals(user)) {
      except(Response.Status.NOT_FOUND,
              "Message not found.");
    }
  }
}
