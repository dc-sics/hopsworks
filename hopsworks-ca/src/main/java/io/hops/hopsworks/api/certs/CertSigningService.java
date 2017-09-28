package io.hops.hopsworks.api.certs;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.hops.hopsworks.api.annotation.AllowCORS;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.kafka.CsrDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.PKIUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;

@Path("/agentservice")
@Stateless
@RolesAllowed({"AGENT", "CLUSTER_AGENT"})
@Api(value = "/agentservice", description = "Agent service")
public class CertSigningService {

  final static Logger logger = Logger.getLogger(CertSigningService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HostEJB hostEJB;

  @POST
  @Path("/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response register(@Context HttpServletRequest req, String jsonString)
          throws AppException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = PKIUtils.signCertificate(csr, settings.
                getIntermediateCaDir(), settings.getHopsworksMasterPasswordSsl(),
                true);
        caPubCert = Files.toString(new File(settings.getIntermediateCaDir()
                + "/certs/ca-chain.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        Logger.getLogger(CertSigningService.class.getName()).log(Level.SEVERE,
                null,
                ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ex.toString());
      }
    }

    if (json.has("host-id") && json.has("agent-password")) {
      String hostId = json.getString("host-id");
      Host host;
      try {
        host = hostEJB.findByHostId(hostId);
        String agentPassword = json.getString("agent-password");
        host.setAgentPassword(agentPassword);
        host.setRegistered(true);
        hostEJB.storeHost(host, true);
      } catch (Exception ex) {
        Logger.getLogger(CertSigningService.class.getName()).log(Level.SEVERE,
                null,
                ex);
      }
    }

    CsrDTO dto = new CsrDTO(caPubCert, pubAgentCert, settings.getHadoopVersionedDir());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }

  @POST
  @Path("/hopsworks")
  @AllowCORS
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response hopsworks(@Context HttpServletRequest req, String jsonString)
          throws AppException {
    JSONObject json = new JSONObject(jsonString);
    String pubAgentCert = "no certificate";
    String caPubCert = "no certificate";
    if (json.has("csr")) {
      String csr = json.getString("csr");
      try {
        pubAgentCert = PKIUtils.signCertificate(csr, settings.
                getCaDir(), settings.getHopsworksMasterPasswordSsl(), false);
        caPubCert = Files.toString(new File(settings.getCaDir()
                + "/certs/ca.cert.pem"), Charsets.UTF_8);
      } catch (IOException | InterruptedException ex) {
        Logger.getLogger(CertSigningService.class.getName()).log(Level.SEVERE,
                null,
                ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ex.toString());
      }
    }

    CsrDTO dto = new CsrDTO(caPubCert, pubAgentCert, settings.getHadoopVersionedDir());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            dto).build();
  }

//  @POST
//  @Path("/addUserToProject")
//  @Consumes(MediaType.APPLICATION_JSON)
//  @Produces(MediaType.APPLICATION_JSON)
//  public Response addUserToProject(@Context HttpServletRequest req,
//          UserCertCreationReqDTO userCert)
//          throws AppException {
//
//    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
//            entity(
//                    dto).build();
//  }

}
