package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Stateless
public class PathValidatorV2 {
  
  private static final Logger LOGGER = Logger.getLogger(PathValidatorV2.class.getName());
  
  @EJB
  private DatasetController datasetContoller;
  
  public Path getFullPath(DataSetPath path){
    String relativePath = path.getRelativePath();
    Path datasetPath = datasetContoller.getDatasetPath(path.getDataSet());
    Path toReturn;
    if (relativePath.isEmpty() || "/".equals(relativePath)){
      toReturn = datasetPath;
    } else {
      //Strip leading slashes
      while (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
      }
      toReturn = new Path(datasetPath, relativePath);
    }
    
    LOGGER.info("XXX: Parent: " + datasetPath + " child: " + relativePath + " combined: " + toReturn);
      
    return toReturn;
  }
  
  public Inode exists(DataSetPath path, InodeFacade ifacade,
      Boolean dir) throws AppException {
    Path fullPath = getFullPath(path);
    Inode inode = ifacade.getInodeAtPath(fullPath.toString());
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_NOT_FOUND);
    }
    
    if (dir != null && dir && !inode.isDir()){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_NOT_DIRECTORY);
    }
    
    if (dir != null && !dir && inode.isDir()){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PATH_IS_DIRECTORY);
    }
    
    return inode;
  }
}

