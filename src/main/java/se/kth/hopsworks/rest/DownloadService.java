package se.kth.hopsworks.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DownloadService {

  private static final Logger LOG
          = Logger.getLogger(DownloadService.class.getName());

  @EJB
  private DistributedFsService dfs;

  private String path;
  private String username;

  public DownloadService() {
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response downloadFromHDFS() throws AppException, AccessControlException {
    FSDataInputStream stream;
    DistributedFileSystemOps dfso = null;
    try {
      if (username != null) {
        dfso = dfs.getDfsOps(username);
        stream = dfso.open(new Path(this.path));
      } else {
        dfso = dfs.getDfsOps();
        stream = dfso.open(new Path(this.path));
      }    
      Response.ResponseBuilder response = Response.ok(buildOutputStream(stream,
              dfso));
      response.header("Content-disposition", "attachment;");
      
      return response.build();
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "File does not exist: " + this.path);
    } 
  }
  
  private StreamingOutput buildOutputStream(final FSDataInputStream stream,
          final DistributedFileSystemOps dfso){
     StreamingOutput output = new StreamingOutput() {
        @Override
        public void write(OutputStream out) throws IOException,
                WebApplicationException {
          int length;
          byte[] buffer = new byte[1024];
          while((length = stream.read(buffer)) != -1) {
            out.write(buffer, 0, length);
          }
          out.flush();
          stream.close();
          dfso.close();
        }
      };
      
      return output;
  }
}
