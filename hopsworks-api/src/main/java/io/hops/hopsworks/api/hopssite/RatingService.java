package io.hops.hopsworks.api.hopssite;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.dto.hopssite.RateDTO;
import io.hops.hopsworks.dela.dto.hopssite.RatingDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopsSite;
import io.hops.hopsworks.dela.hopssite.HopsSiteController;
import io.hops.hopsworks.util.SettingsHelper;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RatingService {

  private final static Logger LOG = Logger.getLogger(RatingService.class.getName());
  @EJB
  private HopsSiteController hopsSite;
  @EJB
  private UserManager userBean;
  @EJB
  private Settings settings;
  @EJB
  private NoCacheResponse noCacheResponse;

  private String publicDSId;

  public RatingService() {
  }

  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  @GET
  public Response getDatasetAllRating() throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all {0}", publicDSId);
    RatingDTO rating = hopsSite.getDatasetAllRating(publicDSId);
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  @GET
  @Path("/user")
  public Response getDatasetUserRating(@Context SecurityContext sc) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userBean, sc.getUserPrincipal().getName());
    RatingDTO rating = hopsSite.performAsUser(user, new HopsSite.UserFunc<RatingDTO>() {
      @Override
      public RatingDTO perform() throws ThirdPartyException {
        return hopsSite.getDatasetUserRating(publicCId, publicDSId, user.getEmail());
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(rating).build();
  }

  @POST
  @Path("{rating}")
  public Response addRating(@Context SecurityContext sc, @PathParam("rating") Integer rating) 
    throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add {0}", publicDSId);
    String publicCId = SettingsHelper.clusterId(settings);
    Users user = SettingsHelper.getUser(userBean, sc.getUserPrincipal().getName());
    hopsSite.performAsUser(user, new HopsSite.UserFunc<String>() {
      @Override
      public String perform() throws ThirdPartyException {
        RateDTO datasetRate = new RateDTO(user.getEmail(), rating);
        hopsSite.addRating(publicCId, publicDSId, datasetRate);
        return "ok";
      }
    });
    LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add - done {0}", publicDSId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
